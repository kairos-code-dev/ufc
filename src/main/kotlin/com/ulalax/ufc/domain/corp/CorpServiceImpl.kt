package com.ulalax.ufc.domain.corp

import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import com.ulalax.ufc.infrastructure.util.CacheHelper
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

/**
 * Corp 도메인 서비스 구현체
 *
 * 책임:
 * - 오케스트레이션 (캐시 → HTTP → 파싱)
 * - 도메인 검증
 * - JSON 파싱 (지역성 원칙: 관련 로직을 가까이 배치)
 *
 * 의존성:
 * - CorpHttpClient (인터페이스): 테스트 격리 가능
 * - CacheHelper (구체 클래스): 인메모리로 충분히 빠름
 *
 * 도메인 순수성:
 * - Ktor HttpClient에 직접 의존하지 않음
 * - CorpHttpClient 인터페이스만 의존 (의존성 역전)
 *
 * 문맥의 지역성:
 * - 파싱 로직이 Service 내부에 위치
 * - 별도 Parser 클래스를 만들지 않음 (구현체 하나, YAGNI)
 *
 * @property httpClient HTTP 통신 인터페이스 (테스트 시 Fake로 교체 가능)
 * @property cache 캐싱 유틸리티
 */
class CorpServiceImpl(
    private val httpClient: CorpHttpClient,
    private val cache: CacheHelper
) : CorpService {

    companion object {
        private val logger = LoggerFactory.getLogger(CorpServiceImpl::class.java)

        // 캐시 TTL 설정: 기업 행동 정보는 일단위로 업데이트되므로 24시간
        private val CORPORATE_ACTIONS_TTL = 24.hours
    }

    // ============================================================================
    // Public API Methods
    // ============================================================================

    override suspend fun getDividends(
        symbol: String,
        period: Period
    ): DividendHistory {
        validateSymbol(symbol)
        val corporateAction = getCorporateAction(symbol, period)
        return DividendHistory(
            symbol = symbol,
            dividends = corporateAction.dividends
        )
    }

    override suspend fun getSplits(
        symbol: String,
        period: Period
    ): SplitHistory {
        validateSymbol(symbol)
        val corporateAction = getCorporateAction(symbol, period)
        return SplitHistory(
            symbol = symbol,
            splits = corporateAction.splits
        )
    }

    override suspend fun getCapitalGains(
        symbol: String,
        period: Period
    ): CapitalGainHistory {
        validateSymbol(symbol)
        val corporateAction = getCorporateAction(symbol, period)
        return CapitalGainHistory(
            symbol = symbol,
            capitalGains = corporateAction.capitalGains
        )
    }

    // ============================================================================
    // Private: 오케스트레이션
    // ============================================================================

    /**
     * 통합 기업 행동 데이터를 조회합니다.
     * 캐시를 먼저 확인하고, 없으면 Chart API에서 데이터를 가져옵니다.
     */
    private suspend fun getCorporateAction(
        symbol: String,
        period: Period
    ): CorporateAction {
        logger.debug("Fetching corporate actions: symbol={}, period={}", symbol, period.value)

        return cache.getOrPut("corp:${symbol.uppercase()}:${period.value}", ttl = CORPORATE_ACTIONS_TTL) {
            // Chart API 호출 (events 포함)
            val chartResponse = httpClient.fetchChartData(
                symbol = symbol,
                interval = Interval.OneDay,
                period = period,
                includeEvents = true
            )

            // 응답 검증
            val result = chartResponse.chart.result?.firstOrNull()
                ?: return@getOrPut createEmptyCorporateAction(symbol, period)

            val events = result.events
            val meta = result.meta

            // 이벤트 파싱
            val dividends = parseDividends(events?.dividends, meta?.currency)
            val splits = parseSplits(events?.splits)
            val capitalGains = parseCapitalGains(events?.capitalGains, meta?.currency)

            // 메타데이터 생성
            val metadata = CorporateActionMetadata(
                symbol = symbol,
                period = period.value,
                currency = meta?.currency,
                assetType = null,  // Asset type 정보가 필요하면 별도로 조회
                fetchedAt = System.currentTimeMillis(),
                source = "YahooFinance"
            )

            // CorporateAction 생성
            CorporateAction(
                symbol = symbol,
                dividends = dividends,
                splits = splits,
                capitalGains = capitalGains,
                metadata = metadata
            )
        }
    }

    // ============================================================================
    // Private: JSON 파싱 (지역성 원칙)
    // ============================================================================

    /**
     * 배당금 이벤트를 Domain 모델로 변환합니다.
     */
    private fun parseDividends(
        events: Map<String, com.ulalax.ufc.domain.chart.DividendEvent>?,
        currency: String?
    ): List<Dividend> {
        if (events == null || events.isEmpty()) {
            return emptyList()
        }

        return events.values
            .filter { it.amount != null && (it.amount ?: 0.0) > 0.0 }
            .map { event ->
                Dividend(
                    date = event.date ?: 0L,
                    amount = event.amount ?: 0.0,
                    currency = currency
                )
            }
            .sortedBy { it.date }
    }

    /**
     * 주식 분할 이벤트를 Domain 모델로 변환합니다.
     */
    private fun parseSplits(
        events: Map<String, com.ulalax.ufc.domain.chart.SplitEvent>?
    ): List<Split> {
        if (events == null || events.isEmpty()) {
            return emptyList()
        }

        return events.values
            .filter { event ->
                val numerator = event.numerator ?: 0.0
                val denominator = event.denominator ?: 1.0
                // ratio가 1.0이 아닌 경우만 (실제 분할)
                denominator != 0.0 && (numerator / denominator) != 1.0
            }
            .map { event ->
                Split(
                    date = event.date ?: 0L,
                    numerator = (event.numerator ?: 0.0).toInt(),
                    denominator = (event.denominator ?: 1.0).toInt(),
                    ratio = (event.numerator ?: 0.0) / (event.denominator ?: 1.0)
                )
            }
            .sortedBy { it.date }
    }

    /**
     * 자본이득 이벤트를 Domain 모델로 변환합니다.
     */
    private fun parseCapitalGains(
        events: Map<String, com.ulalax.ufc.domain.chart.CapitalGainEvent>?,
        currency: String?
    ): List<CapitalGain> {
        if (events == null || events.isEmpty()) {
            return emptyList()
        }

        return events.values
            .filter { it.amount != null && (it.amount ?: 0.0) > 0.0 }
            .map { event ->
                CapitalGain(
                    date = event.date ?: 0L,
                    amount = event.amount ?: 0.0,
                    currency = currency
                )
            }
            .sortedBy { it.date }
    }

    /**
     * 빈 CorporateAction을 생성합니다.
     */
    private fun createEmptyCorporateAction(
        symbol: String,
        period: Period
    ): CorporateAction {
        val metadata = CorporateActionMetadata(
            symbol = symbol,
            period = period.value,
            currency = null,
            assetType = null,
            fetchedAt = System.currentTimeMillis(),
            source = "YahooFinance"
        )
        return CorporateAction(
            symbol = symbol,
            dividends = emptyList(),
            splits = emptyList(),
            capitalGains = emptyList(),
            metadata = metadata
        )
    }

    // ============================================================================
    // Private: 검증
    // ============================================================================

    /**
     * 심볼 검증
     */
    private fun validateSymbol(symbol: String) {
        if (symbol.isBlank()) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "심볼이 비어있습니다")
        }

        if (symbol.length > 20) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "심볼이 너무 깁니다: $symbol (최대 20자)")
        }

        // 유효한 문자: 영문, 숫자, ^, ., -, _
        val validPattern = Regex("^[A-Za-z0-9^.\\-_]+$")
        if (!validPattern.matches(symbol)) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "유효하지 않은 심볼: $symbol")
        }
    }
}
