package com.ulalax.ufc.domain.funds

/**
 * 펀드의 기본 개요 정보.
 *
 * 펀드의 카테고리, 운영사(패밀리), 법적 형태 등의 정보를 담습니다.
 *
 * @property categoryName 펀드 카테고리 (예: "Large Blend", "High Yield Bond", "Emerging Markets")
 * @property family 펀드 운영사/패밀리 (예: "Vanguard", "iShares", "SPDR State Street Global Advisors")
 * @property legalType 법적 형태 (예: "Exchange Traded Fund", "Open Ended Mutual Fund")
 */
data class FundOverview(
    val categoryName: String?,
    val family: String?,
    val legalType: String?
)
