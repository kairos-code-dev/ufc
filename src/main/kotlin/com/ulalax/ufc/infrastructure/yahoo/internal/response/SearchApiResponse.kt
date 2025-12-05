package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Yahoo Finance Search API의 응답을 나타내는 내부 모델
 *
 * 이 모델은 API로부터 받은 JSON을 역직렬화하기 위한 용도로만 사용되며,
 * 클라이언트에는 domain 모델인 SearchResponse로 변환되어 반환됩니다.
 */
@Serializable
internal data class SearchApiResponse(
    /**
     * 총 결과 개수
     */
    @SerialName("count")
    val count: Int? = null,

    /**
     * 종목 검색 결과 리스트
     */
    @SerialName("quotes")
    val quotes: List<SearchQuoteResult>? = null,

    /**
     * 뉴스 검색 결과 리스트
     */
    @SerialName("news")
    val news: List<NewsResult>? = null
)

/**
 * 검색된 종목 정보 (Internal)
 */
@Serializable
internal data class SearchQuoteResult(
    /**
     * 티커 심볼
     */
    @SerialName("symbol")
    val symbol: String? = null,

    /**
     * 짧은 이름
     */
    @SerialName("shortname")
    val shortname: String? = null,

    /**
     * 전체 이름
     */
    @SerialName("longname")
    val longname: String? = null,

    /**
     * 자산 유형 (EQUITY, ETF, MUTUALFUND 등)
     */
    @SerialName("quoteType")
    val quoteType: String? = null,

    /**
     * 거래소 코드 (NMS, NYQ 등)
     */
    @SerialName("exchange")
    val exchange: String? = null,

    /**
     * 거래소 표시명
     */
    @SerialName("exchDisp")
    val exchDisp: String? = null,

    /**
     * 섹터 (주식만 해당)
     */
    @SerialName("sector")
    val sector: String? = null,

    /**
     * 산업 (주식만 해당)
     */
    @SerialName("industry")
    val industry: String? = null,

    /**
     * 검색 관련도 점수
     */
    @SerialName("score")
    val score: Double? = null
)

/**
 * 검색된 뉴스 정보 (Internal)
 */
@Serializable
internal data class NewsResult(
    /**
     * 뉴스 고유 ID
     */
    @SerialName("uuid")
    val uuid: String? = null,

    /**
     * 뉴스 제목
     */
    @SerialName("title")
    val title: String? = null,

    /**
     * 발행사
     */
    @SerialName("publisher")
    val publisher: String? = null,

    /**
     * 뉴스 링크 URL
     */
    @SerialName("link")
    val link: String? = null,

    /**
     * Unix 타임스탬프 (초 단위)
     */
    @SerialName("providerPublishTime")
    val providerPublishTime: Long? = null,

    /**
     * 뉴스 타입 (STORY, VIDEO 등)
     */
    @SerialName("type")
    val type: String? = null,

    /**
     * 썸네일 이미지
     */
    @SerialName("thumbnail")
    val thumbnail: NewsThumbnailResult? = null,

    /**
     * 관련 티커 심볼 리스트
     */
    @SerialName("relatedTickers")
    val relatedTickers: List<String>? = null
)

/**
 * 뉴스 썸네일 이미지 정보 (Internal)
 */
@Serializable
internal data class NewsThumbnailResult(
    /**
     * 다양한 해상도의 이미지 URL 리스트
     */
    @SerialName("resolutions")
    val resolutions: List<ThumbnailResolutionResult>? = null
)

/**
 * 썸네일 이미지 해상도별 정보 (Internal)
 */
@Serializable
internal data class ThumbnailResolutionResult(
    /**
     * 이미지 URL
     */
    @SerialName("url")
    val url: String? = null,

    /**
     * 이미지 너비 (픽셀)
     */
    @SerialName("width")
    val width: Int? = null,

    /**
     * 이미지 높이 (픽셀)
     */
    @SerialName("height")
    val height: Int? = null,

    /**
     * 태그 (예: "140x140", "original")
     */
    @SerialName("tag")
    val tag: String? = null
)
