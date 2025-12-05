package com.ulalax.ufc.fred.model

import java.time.LocalDate

/**
 * FRED 시계열 데이터를 나타냅니다.
 *
 * 시계열 메타데이터와 관측값(observations)을 포함합니다.
 *
 * @property id FRED 시계열 ID (예: "GDP", "UNRATE")
 * @property title 시계열 제목
 * @property frequency 데이터 주기 (예: "Daily", "Monthly", "Quarterly")
 * @property units 단위 (예: "Billions of Dollars", "Percent")
 * @property observations 관측값 목록
 */
data class FredSeries(
    val id: String,
    val title: String,
    val frequency: String,
    val units: String,
    val observations: List<FredObservation>
)

/**
 * FRED 관측값(데이터 포인트)을 나타냅니다.
 *
 * @property date 관측 날짜
 * @property value 관측값 (데이터가 없으면 null)
 */
data class FredObservation(
    val date: LocalDate,
    val value: Double?  // "." 값은 null로 표현
)

/**
 * FRED 시계열 메타데이터만 포함하는 정보입니다.
 *
 * 관측값 없이 시계열의 기본 정보만 조회할 때 사용합니다.
 *
 * @property id FRED 시계열 ID
 * @property title 시계열 제목
 * @property frequency 데이터 주기
 * @property units 단위
 * @property seasonalAdjustment 계절 조정 여부
 * @property lastUpdated 마지막 업데이트 시간
 */
data class FredSeriesInfo(
    val id: String,
    val title: String,
    val frequency: String,
    val units: String,
    val seasonalAdjustment: String?,
    val lastUpdated: String?
)
