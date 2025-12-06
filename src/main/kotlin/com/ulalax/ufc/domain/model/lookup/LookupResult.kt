package com.ulalax.ufc.domain.model.lookup

/**
 * Yahoo Finance Lookup API 검색 결과
 *
 * 검색 쿼리에 대한 금융상품 검색 결과를 포함합니다.
 *
 * @property query 검색 키워드
 * @property type 검색에 사용된 타입 필터
 * @property count 반환된 결과 개수 (실제 documents.size와 동일)
 * @property start 페이징 시작 인덱스 (0부터 시작)
 * @property total 전체 검색 결과 개수
 * @property documents 검색된 금융상품 목록
 */
data class LookupResult(
    val query: String,
    val type: LookupType,
    val count: Int,
    val start: Int,
    val total: Int,
    val documents: List<LookupDocument>,
)
