package com.ulalax.ufc.domain.model.search

/**
 * 썸네일 이미지 해상도별 정보
 *
 * @property url 이미지 URL
 * @property width 이미지 너비 (픽셀)
 * @property height 이미지 높이 (픽셀)
 * @property tag 태그 (예: "140x140", "original")
 */
data class ThumbnailResolution(
    val url: String,
    val width: Int,
    val height: Int,
    val tag: String,
)
