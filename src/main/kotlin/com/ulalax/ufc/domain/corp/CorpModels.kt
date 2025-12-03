package com.ulalax.ufc.domain.corp

/**
 * 배당금 이벤트
 *
 * @property date 배당 기준일 (Unix timestamp, seconds)
 * @property amount 배당금액 (주당)
 * @property currency 통화 코드 (선택적)
 */
data class Dividend(
    val date: Long,
    val amount: Double,
    val currency: String? = null
)

/**
 * 주식 분할 이벤트
 *
 * @property date 분할 기준일 (Unix timestamp, seconds)
 * @property numerator 분자 (예: 4:1 분할에서 4)
 * @property denominator 분모 (예: 4:1 분할에서 1)
 * @property ratio 분할 비율 (numerator/denominator)
 */
data class Split(
    val date: Long,
    val numerator: Int,
    val denominator: Int,
    val ratio: Double
) {
    /**
     * 분할 비율을 문자열로 반환합니다.
     * 예: "4:1", "1:2"
     */
    fun ratioString(): String = "$numerator:$denominator"

    /**
     * 분할 설명을 반환합니다.
     * 예: "4-for-1 split"
     */
    fun description(): String = "$numerator-for-$denominator split"
}

/**
 * 자본이득 분배 이벤트
 *
 * @property date 분배 기준일 (Unix timestamp, seconds)
 * @property amount 자본이득 분배액 (주당)
 * @property currency 통화 코드 (선택적)
 */
data class CapitalGain(
    val date: Long,
    val amount: Double,
    val currency: String? = null
)

/**
 * 배당금 히스토리
 *
 * @property symbol 심볼
 * @property dividends 배당금 목록 (날짜 오름차순 정렬)
 */
data class DividendHistory(
    val symbol: String,
    val dividends: List<Dividend>
)

/**
 * 주식 분할 히스토리
 *
 * @property symbol 심볼
 * @property splits 주식 분할 목록 (날짜 오름차순 정렬)
 */
data class SplitHistory(
    val symbol: String,
    val splits: List<Split>
)

/**
 * 자본이득 분배 히스토리
 *
 * @property symbol 심볼
 * @property capitalGains 자본이득 목록 (날짜 오름차순 정렬)
 */
data class CapitalGainHistory(
    val symbol: String,
    val capitalGains: List<CapitalGain>
)

/**
 * 통합 기업 행동 (Corporate Actions)
 *
 * 배당금, 주식 분할, 자본이득을 하나의 데이터 구조로 제공합니다.
 *
 * @property symbol 심볼
 * @property dividends 배당금 목록 (빈 리스트 가능)
 * @property splits 주식 분할 목록 (빈 리스트 가능, MUTUALFUND는 항상 빈 리스트)
 * @property capitalGains 자본이득 목록 (빈 리스트 가능)
 * @property metadata 메타데이터
 */
data class CorporateAction(
    val symbol: String,
    val dividends: List<Dividend>,
    val splits: List<Split>,
    val capitalGains: List<CapitalGain>,
    val metadata: CorporateActionMetadata
)

/**
 * 기업 행동 메타데이터
 *
 * @property symbol 심볼
 * @property period 조회 기간
 * @property currency 통화 코드
 * @property assetType 자산 유형 (선택적)
 * @property fetchedAt 조회 시각 (Unix timestamp, millis)
 * @property source 데이터 소스
 */
data class CorporateActionMetadata(
    val symbol: String,
    val period: String,
    val currency: String? = null,
    val assetType: String? = null,
    val fetchedAt: Long,
    val source: String = "YahooFinance"
)
