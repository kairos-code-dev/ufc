package com.ulalax.ufc.unit.util

import com.ulalax.ufc.domain.chart.OHLCVData
import com.ulalax.ufc.infrastructure.util.TechnicalIndicators
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.abs

/**
 * TechnicalIndicators 유틸리티 테스트
 *
 * SMA(단순 이동평균)와 EMA(지수 이동평균) 계산의 정확성을 검증합니다.
 */
class TechnicalIndicatorsTest {

    companion object {
        // 부동소수점 비교를 위한 오차 범위
        private const val DELTA = 0.01

        // 테스트용 가격 데이터
        private val TEST_PRICES = listOf(10.0, 11.0, 12.0, 11.0, 13.0, 14.0, 13.0, 15.0)
    }

    // ===== SMA 테스트 =====

    @Test
    fun `calculateSMA - period 3으로 정상 계산`() {
        // Given
        val prices = TEST_PRICES
        val period = 3

        // When
        val result = TechnicalIndicators.calculateSMA(prices, period)

        // Then
        assertEquals(prices.size, result.size, "결과 리스트 크기는 입력과 같아야 함")

        // 처음 2개는 null (period - 1)
        assertNull(result[0])
        assertNull(result[1])

        // 이후 값들 검증
        // [10, 11, 12] = 33/3 = 11.0
        assertNotNull(result[2])
        assertEquals(11.0, result[2]!!, DELTA)

        // [11, 12, 11] = 34/3 = 11.33
        assertNotNull(result[3])
        assertEquals(11.33, result[3]!!, DELTA)

        // [12, 11, 13] = 36/3 = 12.0
        assertNotNull(result[4])
        assertEquals(12.0, result[4]!!, DELTA)

        // [11, 13, 14] = 38/3 = 12.67
        assertNotNull(result[5])
        assertEquals(12.67, result[5]!!, DELTA)

        // [13, 14, 13] = 40/3 = 13.33
        assertNotNull(result[6])
        assertEquals(13.33, result[6]!!, DELTA)

        // [14, 13, 15] = 42/3 = 14.0
        assertNotNull(result[7])
        assertEquals(14.0, result[7]!!, DELTA)
    }

    @Test
    fun `calculateSMA - period 5로 정상 계산`() {
        // Given
        val prices = TEST_PRICES
        val period = 5

        // When
        val result = TechnicalIndicators.calculateSMA(prices, period)

        // Then
        assertEquals(prices.size, result.size)

        // 처음 4개는 null
        (0..3).forEach { assertNull(result[it]) }

        // [10, 11, 12, 11, 13] = 57/5 = 11.4
        assertNotNull(result[4])
        assertEquals(11.4, result[4]!!, DELTA)

        // [11, 12, 11, 13, 14] = 61/5 = 12.2
        assertNotNull(result[5])
        assertEquals(12.2, result[5]!!, DELTA)

        // [12, 11, 13, 14, 13] = 63/5 = 12.6
        assertNotNull(result[6])
        assertEquals(12.6, result[6]!!, DELTA)

        // [11, 13, 14, 13, 15] = 66/5 = 13.2
        assertNotNull(result[7])
        assertEquals(13.2, result[7]!!, DELTA)
    }

    @Test
    fun `calculateSMA - 빈 리스트 처리`() {
        // Given
        val emptyList = emptyList<Double>()
        val period = 3

        // When
        val result = TechnicalIndicators.calculateSMA(emptyList, period)

        // Then
        assertTrue(result.isEmpty(), "빈 리스트 입력 시 빈 리스트 반환")
    }

    @Test
    fun `calculateSMA - period보다 작은 데이터`() {
        // Given
        val prices = listOf(10.0, 11.0)
        val period = 5

        // When
        val result = TechnicalIndicators.calculateSMA(prices, period)

        // Then
        assertEquals(2, result.size, "결과 리스트 크기는 입력과 같아야 함")
        assertNull(result[0], "데이터가 period보다 작으면 모두 null")
        assertNull(result[1], "데이터가 period보다 작으면 모두 null")
    }

    @Test
    fun `calculateSMA - 단일 값 처리`() {
        // Given
        val singlePrice = listOf(10.0)
        val period = 1

        // When
        val result = TechnicalIndicators.calculateSMA(singlePrice, period)

        // Then
        assertEquals(1, result.size)
        assertNotNull(result[0])
        assertEquals(10.0, result[0]!!, DELTA)
    }

    @Test
    fun `calculateSMA - period가 0이면 예외 발생`() {
        // Given
        val prices = listOf(10.0, 11.0, 12.0)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TechnicalIndicators.calculateSMA(prices, 0)
        }
        assertTrue(exception.message!!.contains("Period must be greater than 0"))
    }

    @Test
    fun `calculateSMA - period가 음수면 예외 발생`() {
        // Given
        val prices = listOf(10.0, 11.0, 12.0)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TechnicalIndicators.calculateSMA(prices, -5)
        }
        assertTrue(exception.message!!.contains("Period must be greater than 0"))
    }

    // ===== EMA 테스트 =====

    @Test
    fun `calculateEMA - period 3으로 정상 계산`() {
        // Given
        val prices = TEST_PRICES
        val period = 3
        val multiplier = 2.0 / (period + 1) // 0.5

        // When
        val result = TechnicalIndicators.calculateEMA(prices, period)

        // Then
        assertEquals(prices.size, result.size)

        // 처음 2개는 null
        assertNull(result[0])
        assertNull(result[1])

        // 첫 EMA는 SMA로 시작: [10, 11, 12] = 11.0
        assertNotNull(result[2])
        assertEquals(11.0, result[2]!!, DELTA)

        // EMA[3] = (11 × 0.5) + (11.0 × 0.5) = 11.0
        assertNotNull(result[3])
        assertEquals(11.0, result[3]!!, DELTA)

        // EMA[4] = (13 × 0.5) + (11.0 × 0.5) = 12.0
        assertNotNull(result[4])
        assertEquals(12.0, result[4]!!, DELTA)

        // EMA[5] = (14 × 0.5) + (12.0 × 0.5) = 13.0
        assertNotNull(result[5])
        assertEquals(13.0, result[5]!!, DELTA)

        // EMA[6] = (13 × 0.5) + (13.0 × 0.5) = 13.0
        assertNotNull(result[6])
        assertEquals(13.0, result[6]!!, DELTA)

        // EMA[7] = (15 × 0.5) + (13.0 × 0.5) = 14.0
        assertNotNull(result[7])
        assertEquals(14.0, result[7]!!, DELTA)
    }

    @Test
    fun `calculateEMA - period 5로 정상 계산`() {
        // Given
        val prices = TEST_PRICES
        val period = 5
        val multiplier = 2.0 / (period + 1) // 0.333...

        // When
        val result = TechnicalIndicators.calculateEMA(prices, period)

        // Then
        assertEquals(prices.size, result.size)

        // 처음 4개는 null
        (0..3).forEach { assertNull(result[it]) }

        // 첫 EMA는 SMA: [10, 11, 12, 11, 13] = 11.4
        assertNotNull(result[4])
        assertEquals(11.4, result[4]!!, DELTA)

        // EMA[5] = (14 × 0.333) + (11.4 × 0.667) = 4.667 + 7.6 = 12.27
        assertNotNull(result[5])
        val expected5 = (14.0 * multiplier) + (11.4 * (1 - multiplier))
        assertEquals(expected5, result[5]!!, DELTA)

        // 연속 계산 검증
        var previousEMA = result[4]!!
        for (i in 5 until result.size) {
            val expectedEMA = (prices[i] * multiplier) + (previousEMA * (1 - multiplier))
            assertEquals(expectedEMA, result[i]!!, DELTA, "EMA[$i] 계산 오류")
            previousEMA = result[i]!!
        }
    }

    @Test
    fun `calculateEMA - 빈 리스트 처리`() {
        // Given
        val emptyList = emptyList<Double>()
        val period = 3

        // When
        val result = TechnicalIndicators.calculateEMA(emptyList, period)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateEMA - period보다 작은 데이터`() {
        // Given
        val prices = listOf(10.0, 11.0)
        val period = 5

        // When
        val result = TechnicalIndicators.calculateEMA(prices, period)

        // Then
        assertEquals(2, result.size)
        assertNull(result[0])
        assertNull(result[1])
    }

    @Test
    fun `calculateEMA - 단일 값 처리`() {
        // Given
        val singlePrice = listOf(10.0)
        val period = 1

        // When
        val result = TechnicalIndicators.calculateEMA(singlePrice, period)

        // Then
        assertEquals(1, result.size)
        assertNotNull(result[0])
        assertEquals(10.0, result[0]!!, DELTA)
    }

    @Test
    fun `calculateEMA - period가 0이면 예외 발생`() {
        // Given
        val prices = listOf(10.0, 11.0, 12.0)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TechnicalIndicators.calculateEMA(prices, 0)
        }
        assertTrue(exception.message!!.contains("Period must be greater than 0"))
    }

    @Test
    fun `calculateEMA - period가 음수면 예외 발생`() {
        // Given
        val prices = listOf(10.0, 11.0, 12.0)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            TechnicalIndicators.calculateEMA(prices, -5)
        }
        assertTrue(exception.message!!.contains("Period must be greater than 0"))
    }

    @Test
    fun `calculateEMA - EMA는 최근 가격에 더 민감하게 반응`() {
        // Given
        val prices = listOf(10.0, 10.0, 10.0, 10.0, 10.0, 20.0) // 마지막에 급등
        val period = 3

        // When
        val smaResult = TechnicalIndicators.calculateSMA(prices, period)
        val emaResult = TechnicalIndicators.calculateEMA(prices, period)

        // Then
        val lastSMA = smaResult.last()!!
        val lastEMA = emaResult.last()!!

        // EMA가 SMA보다 최근 가격(20.0)에 더 가까워야 함
        assertTrue(abs(lastEMA - 20.0) < abs(lastSMA - 20.0),
            "EMA($lastEMA)가 SMA($lastSMA)보다 최근 가격(20.0)에 더 가까워야 함")
    }

    // ===== OHLCV 기반 계산 테스트 =====

    @Test
    fun `calculateSMAFromOHLCV - OHLCV 데이터로부터 SMA 계산`() {
        // Given
        val ohlcvList = TEST_PRICES.mapIndexed { index, close ->
            OHLCVData(
                timestamp = 1700000000L + index * 86400,
                open = close - 0.5,
                high = close + 0.5,
                low = close - 1.0,
                close = close,
                adjClose = close,
                volume = 1000000L
            )
        }
        val period = 3

        // When
        val result = TechnicalIndicators.calculateSMAFromOHLCV(ohlcvList, period)

        // Then
        assertEquals(ohlcvList.size, result.size)

        // 종가 기반 SMA와 동일한 결과여야 함
        val directSMA = TechnicalIndicators.calculateSMA(TEST_PRICES, period)
        result.indices.forEach { index ->
            if (result[index] == null) {
                assertNull(directSMA[index])
            } else {
                assertEquals(directSMA[index]!!, result[index]!!, DELTA)
            }
        }
    }

    @Test
    fun `calculateEMAFromOHLCV - OHLCV 데이터로부터 EMA 계산`() {
        // Given
        val ohlcvList = TEST_PRICES.mapIndexed { index, close ->
            OHLCVData(
                timestamp = 1700000000L + index * 86400,
                open = close - 0.5,
                high = close + 0.5,
                low = close - 1.0,
                close = close,
                adjClose = close,
                volume = 1000000L
            )
        }
        val period = 3

        // When
        val result = TechnicalIndicators.calculateEMAFromOHLCV(ohlcvList, period)

        // Then
        assertEquals(ohlcvList.size, result.size)

        // 종가 기반 EMA와 동일한 결과여야 함
        val directEMA = TechnicalIndicators.calculateEMA(TEST_PRICES, period)
        result.indices.forEach { index ->
            if (result[index] == null) {
                assertNull(directEMA[index])
            } else {
                assertEquals(directEMA[index]!!, result[index]!!, DELTA)
            }
        }
    }

    @Test
    fun `calculateSMAFromOHLCV - 빈 OHLCV 리스트 처리`() {
        // Given
        val emptyList = emptyList<OHLCVData>()
        val period = 3

        // When
        val result = TechnicalIndicators.calculateSMAFromOHLCV(emptyList, period)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateEMAFromOHLCV - 빈 OHLCV 리스트 처리`() {
        // Given
        val emptyList = emptyList<OHLCVData>()
        val period = 3

        // When
        val result = TechnicalIndicators.calculateEMAFromOHLCV(emptyList, period)

        // Then
        assertTrue(result.isEmpty())
    }

    // ===== 엣지 케이스 및 실용성 테스트 =====

    @Test
    fun `calculateSMA - 동일한 가격 데이터는 동일한 SMA 반환`() {
        // Given
        val constantPrices = List(10) { 15.0 }
        val period = 5

        // When
        val result = TechnicalIndicators.calculateSMA(constantPrices, period)

        // Then
        result.drop(period - 1).forEach { value ->
            assertNotNull(value)
            assertEquals(15.0, value!!, DELTA, "동일한 가격은 동일한 SMA")
        }
    }

    @Test
    fun `calculateEMA - 동일한 가격 데이터는 동일한 EMA 반환`() {
        // Given
        val constantPrices = List(10) { 15.0 }
        val period = 5

        // When
        val result = TechnicalIndicators.calculateEMA(constantPrices, period)

        // Then
        result.drop(period - 1).forEach { value ->
            assertNotNull(value)
            assertEquals(15.0, value!!, DELTA, "동일한 가격은 동일한 EMA")
        }
    }

    @Test
    fun `calculateSMA - 큰 데이터셋 처리 성능 테스트`() {
        // Given
        val largePrices = (1..10000).map { it.toDouble() }
        val period = 200

        // When
        val startTime = System.currentTimeMillis()
        val result = TechnicalIndicators.calculateSMA(largePrices, period)
        val endTime = System.currentTimeMillis()

        // Then
        assertEquals(largePrices.size, result.size)
        assertTrue(endTime - startTime < 1000, "10,000개 데이터 처리가 1초 이내")
        assertNull(result[period - 2])
        assertNotNull(result[period - 1])
    }

    @Test
    fun `calculateEMA - 큰 데이터셋 처리 성능 테스트`() {
        // Given
        val largePrices = (1..10000).map { it.toDouble() }
        val period = 200

        // When
        val startTime = System.currentTimeMillis()
        val result = TechnicalIndicators.calculateEMA(largePrices, period)
        val endTime = System.currentTimeMillis()

        // Then
        assertEquals(largePrices.size, result.size)
        assertTrue(endTime - startTime < 1000, "10,000개 데이터 처리가 1초 이내")
        assertNull(result[period - 2])
        assertNotNull(result[period - 1])
    }
}
