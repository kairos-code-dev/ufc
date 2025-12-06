package com.ulalax.ufc.domain.model.search

/**
 * 검색된 뉴스 정보
 *
 * @property uuid 뉴스 고유 ID
 * @property title 뉴스 제목
 * @property publisher 발행사
 * @property link 뉴스 링크 URL
 * @property publishTime Unix 타임스탬프 (초 단위)
 * @property type 뉴스 타입 (STORY, VIDEO 등)
 * @property thumbnail 썸네일 이미지
 * @property relatedTickers 관련 티커 심볼 리스트
 */
data class SearchNews(
    val uuid: String,
    val title: String,
    val publisher: String?,
    val link: String,
    val publishTime: Long,
    val type: String?,
    val thumbnail: NewsThumbnail?,
    val relatedTickers: List<String>,
)
