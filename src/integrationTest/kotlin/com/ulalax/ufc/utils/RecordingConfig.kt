package com.ulalax.ufc.utils

import java.nio.file.Path
import java.nio.file.Paths

/**
 * API 응답 기록 설정
 *
 * 라이브 테스트에서 수신한 API 응답을 로컬 파일로 기록하기 위한 설정을 제공합니다.
 * 이를 통해 나중에 네트워크 접근 없이 기록된 응답을 사용할 수 있습니다.
 *
 * 기록 활성화 여부:
 * - 시스템 프로퍼티 "record.responses"를 통해 제어
 * - 기본값: true (활성화)
 *
 * 사용 예시:
 * ```
 * // 기록 활성화 상태에서 실행
 * // gradle을 사용하는 경우
 * gradle liveTest -Drecord.responses=true
 *
 * // 기록 비활성화 상태에서 실행
 * gradle liveTest -Drecord.responses=false
 * ```
 */
object RecordingConfig {

    /**
     * API 응답 기록이 활성화되어 있는지 여부
     *
     * System.getProperty("record.responses", "true")를 확인합니다.
     */
    val isRecordingEnabled: Boolean
        get() {
            val recordingProperty = System.getProperty("record.responses", "true")
            return recordingProperty?.toBoolean() ?: true
        }

    /**
     * 기록된 응답 파일들의 기본 출력 경로
     *
     * 모든 기록은 이 경로 아래에 카테고리별로 저장됩니다.
     */
    val baseOutputPath: Path = java.nio.file.Paths.get("src/liveTest/resources/responses")

    /**
     * 세부 디렉토리 경로들을 정의하는 중첩 object
     *
     * 각 API 제공자별로 서로 다른 데이터 타입에 대한 경로를 제공합니다.
     */
    object Paths {

        /**
         * Yahoo Finance API 응답 경로들
         */
        object Yahoo {

            /**
             * ETF 관련 데이터 카테고리
             *
             * ETF의 상세 정보와 구성 종목 관련 응답을 기록합니다.
             */
            object Etf {
                const val TOP_HOLDINGS = "yahoo/etf/top_holdings"
                const val SECTOR_WEIGHTINGS = "yahoo/etf/sector_weightings"
                const val ASSET_ALLOCATION = "yahoo/etf/asset_allocation"
                const val FUND_PROFILE = "yahoo/etf/fund_profile"
                const val EQUITY_HOLDINGS = "yahoo/etf/equity_holdings"
                const val BOND_HOLDINGS = "yahoo/etf/bond_holdings"
            }

            /**
             * 개별 주식 관련 데이터 카테고리
             *
             * 주식의 가격, 배당금, 분할, 재무 정보 등을 기록합니다.
             */
            object Ticker {
                const val HISTORY = "yahoo/ticker/history"
                const val DIVIDENDS = "yahoo/ticker/dividends"
                const val SPLITS = "yahoo/ticker/splits"
                const val FINANCIALS = "yahoo/ticker/financials"
                const val INFO = "yahoo/ticker/info"
                const val RECOMMENDATIONS = "yahoo/ticker/recommendations"
            }

            /**
             * 차트 데이터 카테고리
             *
             * 시간 단위별로 가격 데이터를 조회합니다.
             */
            object Chart {
                const val BASE = "yahoo/chart"
                const val INTRADAY = "$BASE/intraday"
                const val DAILY = "$BASE/daily"
                const val WEEKLY = "$BASE/weekly"
                const val MONTHLY = "$BASE/monthly"
                const val ADJUSTED = "$BASE/adjusted"
            }

            /**
             * Quote Summary 데이터 카테고리
             *
             * 주식/ETF의 요약 정보, 가격, 재무 정보 등을 조회합니다.
             */
            object Quote {
                const val BASE = "yahoo/quote"
                const val SUMMARY = "$BASE/summary"
                const val PRICE = "$BASE/price"
                const val FINANCIALS = "$BASE/financials"
            }

            /**
             * 검색 관련 데이터 카테고리
             *
             * 기호 검색 및 스크리너 결과를 기록합니다.
             */
            object Search {
                const val BASIC = "yahoo/search/basic"
                const val SCREENER = "yahoo/search/screener"
            }
        }

        /**
         * FRED (Federal Reserve Economic Data) API 응답 경로들
         *
         * 미국 연방준비제도가 제공하는 경제 지표 데이터를 기록합니다.
         */
        object Fred {
            const val SERIES = "fred/series"
            const val SERIES_INFO = "fred/series_info"
            const val VINTAGE = "fred/vintage"
            const val SEARCH = "fred/search"
            const val SEARCH_BY_CATEGORY = "fred/search_by_category"
            const val SEARCH_BY_RELEASE = "fred/search_by_release"
            const val CATEGORY = "fred/category"
            const val INDICATORS = "fred/indicators"
        }
    }
}
