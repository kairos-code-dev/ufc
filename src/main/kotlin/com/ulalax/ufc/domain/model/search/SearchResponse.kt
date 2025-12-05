package com.ulalax.ufc.domain.model.search

/**
 * Yahoo Finance Search API 응답 데이터
 *
 * 검색어에 대한 종목 및 뉴스 검색 결과를 담고 있습니다.
 *
 * @property query 검색어
 * @property count 총 결과 개수
 * @property quotes 종목 검색 결과 리스트
 * @property news 뉴스 검색 결과 리스트
 */
data class SearchResponse(
    val query: String,
    val count: Int,
    val quotes: List<SearchQuote>,
    val news: List<SearchNews>
)
