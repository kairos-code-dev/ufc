package com.ulalax.ufc.infrastructure.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 날짜 및 시간 유틸리티 클래스입니다.
 */
object DateTimeUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    /**
     * LocalDate를 문자열로 변환합니다.
     */
    fun localDateToString(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    /**
     * 문자열을 LocalDate로 변환합니다.
     */
    fun stringToLocalDate(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, dateFormatter)
    }

    /**
     * LocalDateTime을 문자열로 변환합니다.
     */
    fun localDateTimeToString(dateTime: LocalDateTime): String {
        return dateTime.format(dateTimeFormatter)
    }

    /**
     * 문자열을 LocalDateTime으로 변환합니다.
     */
    fun stringToLocalDateTime(dateTimeStr: String): LocalDateTime {
        return LocalDateTime.parse(dateTimeStr, dateTimeFormatter)
    }

    /**
     * 현재 날짜를 반환합니다.
     */
    fun now(): LocalDate {
        return LocalDate.now()
    }

    /**
     * 현재 날짜/시간을 반환합니다.
     */
    fun nowDateTime(): LocalDateTime {
        return LocalDateTime.now()
    }
}
