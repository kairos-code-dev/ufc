package com.ulalax.ufc.domain.stock

/**
 * 회사 정보 조회 결과의 메타데이터.
 *
 * 캐시 상태, 데이터 완전성, 데이터 출처 등의 정보를 포함합니다.
 */
data class CompanyInfoMetadata(
    val symbol: String,
    val fetchedAt: Long,                  // Unix timestamp (millis)
    val source: String,                   // "YahooFinance"
    val modulesUsed: List<String>,        // ["assetProfile", "quoteType", ...]
    val dataCompleteness: DataCompleteness
)

/**
 * 조회된 데이터의 완전성을 나타내는 지표.
 *
 * 데이터 품질 평가 및 디버깅에 사용됩니다.
 */
data class DataCompleteness(
    val totalFields: Int,             // 전체 필드 수
    val populatedFields: Int,         // 채워진 필드 수
    val completenessPercent: Double   // populatedFields / totalFields * 100
)
