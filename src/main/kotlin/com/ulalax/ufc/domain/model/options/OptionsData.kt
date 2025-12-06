package com.ulalax.ufc.domain.model.options

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Options API 조회 결과
 *
 * Yahoo Finance Options API를 통해 조회한 옵션 체인 데이터를 나타냅니다.
 *
 * 사용 예시:
 * ```kotlin
 * // 기본 조회 (가장 가까운 만기일)
 * val options = ufc.options("AAPL")
 *
 * // 특정 만기일 조회
 * val expiration = 1704326400L  // 2024-01-03
 * val options = ufc.options("AAPL", expiration)
 *
 * // ATM 옵션 찾기
 * val (atmCall, atmPut) = options.findAtTheMoneyOptions()
 *
 * // 가장 가까운 행사가 찾기
 * val targetStrike = 150.0
 * val nearestStrike = options.findNearestStrike(targetStrike)
 * ```
 *
 * @property underlyingSymbol 기초 자산 심볼 (예: "AAPL")
 * @property expirationDates 사용 가능한 모든 만기일 목록 (Unix timestamp, seconds)
 * @property strikes 사용 가능한 모든 행사가 목록
 * @property hasMiniOptions 미니 옵션 존재 여부
 * @property underlyingQuote 기초 자산 가격 정보
 * @property optionsChain 옵션 체인 데이터
 */
data class OptionsData(
    val underlyingSymbol: String,
    val expirationDates: List<Long>,
    val strikes: List<Double>,
    val hasMiniOptions: Boolean,
    val underlyingQuote: UnderlyingQuote?,
    val optionsChain: OptionsChain,
) {
    /**
     * 만기일 목록을 LocalDate로 변환
     *
     * @return LocalDate 목록
     */
    fun getExpirationDatesAsLocalDate(): List<LocalDate> =
        expirationDates.map { timestamp ->
            Instant
                .ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

    /**
     * 목표 행사가에 가장 가까운 행사가 찾기
     *
     * @param targetStrike 목표 행사가
     * @return 가장 가까운 행사가, 행사가 목록이 비어있으면 null
     */
    fun findNearestStrike(targetStrike: Double): Double? {
        if (strikes.isEmpty()) return null
        return strikes.minByOrNull { abs(it - targetStrike) }
    }

    /**
     * ATM(등가격) 옵션 찾기
     *
     * 기초 자산 현재가에 가장 가까운 행사가의 콜/풋 옵션을 반환합니다.
     * underlyingQuote 또는 regularMarketPrice가 없으면 (null, null)을 반환합니다.
     *
     * @return Pair<콜옵션, 풋옵션>, 찾지 못하면 각각 null
     */
    fun findAtTheMoneyOptions(): Pair<OptionContract?, OptionContract?> {
        val currentPrice = underlyingQuote?.regularMarketPrice ?: return Pair(null, null)
        val atmStrike = findNearestStrike(currentPrice) ?: return Pair(null, null)

        val atmCall = optionsChain.findCall(atmStrike)
        val atmPut = optionsChain.findPut(atmStrike)

        return Pair(atmCall, atmPut)
    }

    /**
     * 기초 자산 현재가 반환
     *
     * @return 현재가, 없으면 null
     */
    fun getUnderlyingPrice(): Double? = underlyingQuote?.regularMarketPrice

    /**
     * 특정 만기일이 사용 가능한지 확인
     *
     * @param expirationDate 확인할 만기일 (Unix timestamp, seconds)
     * @return 사용 가능하면 true, 아니면 false
     */
    fun hasExpirationDate(expirationDate: Long): Boolean = expirationDate in expirationDates
}
