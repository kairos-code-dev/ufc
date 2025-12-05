package com.ulalax.ufc.infrastructure.common.http

/**
 * Yahoo Finance API와 통신하기 위한 User-Agent 관리 객체
 * 다양한 브라우저 User-Agent를 제공하여 Yahoo의 봇 탐지를 우회합니다.
 */
object UserAgents {

    /**
     * Chrome 브라우저 User-Agent 목록
     * 다양한 운영 체제에서의 Chrome 사용자 에이전트
     */
    val CHROME = listOf(
        // Windows 10 + Chrome
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        // macOS + Chrome
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        // Linux + Chrome
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    )

    /**
     * Firefox 브라우저 User-Agent 목록
     * 다양한 운영 체제에서의 Firefox 사용자 에이전트
     */
    val FIREFOX = listOf(
        // Windows 10 + Firefox
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
        // macOS + Firefox
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.7; rv:135.0) Gecko/20100101 Firefox/135.0"
    )

    /**
     * Safari 브라우저 User-Agent 목록
     * macOS에서의 Safari 사용자 에이전트
     */
    val SAFARI = listOf(
        // macOS Safari
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Safari/605.1.15"
    )

    /**
     * Edge 브라우저 User-Agent 목록
     * Windows에서의 Microsoft Edge 사용자 에이전트
     */
    val EDGE = listOf(
        // Windows 10 + Edge
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"
    )

    /**
     * 모든 User-Agent를 포함한 리스트
     * CHROME + FIREFOX + SAFARI + EDGE
     */
    val ALL = CHROME + FIREFOX + SAFARI + EDGE

    /**
     * 무작위로 User-Agent를 선택하여 반환합니다.
     * Yahoo Finance API 호출 시마다 다른 브라우저로 위장할 수 있습니다.
     *
     * @return 무작위로 선택된 User-Agent 문자열
     */
    fun random(): String = ALL.random()
}
