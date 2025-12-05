package com.ulalax.ufc.domain.model.search

/**
 * 뉴스 썸네일 이미지 정보
 *
 * @property resolutions 다양한 해상도의 이미지 URL 리스트
 */
data class NewsThumbnail(
    val resolutions: List<ThumbnailResolution>
)
