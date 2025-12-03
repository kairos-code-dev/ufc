package com.ulalax.ufc.domain.stock

/**
 * 회사의 기본 정보를 나타내는 도메인 모델.
 *
 * Yahoo Finance Quote Summary API의 assetProfile, summaryProfile, quoteType, defaultKeyStatistics
 * 모듈로부터 추출된 데이터입니다.
 */
data class CompanyInfo(
    // 기본 식별자
    val symbol: String,
    val longName: String,            // Non-nullable
    val shortName: String?,

    // 분류 정보
    val sector: String?,
    val industry: String?,
    val country: String?,

    // 거래소 정보
    val exchange: String?,
    val currency: String?,
    val quoteType: AssetType?,

    // 연락처 정보
    val website: String?,
    val phone: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,

    // 기업 정보
    val employees: Long?,
    val description: String?,

    // 발행주식수 (경계 케이스)
    val sharesOutstanding: Long?,

    // 메타데이터
    val metadata: CompanyInfoMetadata
)

/**
 * 빠른 조회용 최소한의 정보를 담은 모델.
 *
 * yfinance의 fast_info에 해당합니다.
 * 모든 필드는 Non-nullable입니다.
 */
data class FastInfo(
    val symbol: String,
    val currency: String,            // Non-nullable
    val exchange: String,            // Non-nullable
    val quoteType: AssetType         // Non-nullable
)

/**
 * 발행주식수 히스토리를 나타내는 데이터 포인트.
 *
 * 분기별 또는 사용자 지정 기간의 발행주식수 데이터입니다.
 */
data class SharesData(
    val date: java.time.LocalDate,
    val shares: Long
)
