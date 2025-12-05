package com.ulalax.ufc.common.exception

/**
 * UFC 애플리케이션에서 사용되는 에러 코드 정의.
 *
 * 에러 코드 범위:
 * - 1000번대: 네트워크 오류
 * - 2000번대: 인증 오류
 * - 3000번대: Rate Limiting 오류
 * - 4000번대: 데이터 오류
 * - 5000번대: 파싱 오류
 * - 6000번대: 파라미터 오류
 * - 7000번대: 서버 오류
 * - 9000번대: 기타 오류
 */
enum class ErrorCode(
    val code: Int,
    val message: String,
    val isRetryableFlag: Boolean = false
) {
    // 1000번대: 네트워크 오류
    NETWORK_TIMEOUT(1001, "네트워크 타임아웃이 발생했습니다.", true),
    NETWORK_ERROR(1002, "네트워크 오류가 발생했습니다.", true),
    NETWORK_CONNECTION_ERROR(1002, "네트워크 연결 오류가 발생했습니다.", true),
    NETWORK_DNS_ERROR(1003, "DNS 해석 오류가 발생했습니다.", true),
    NETWORK_UNAVAILABLE(1004, "네트워크를 사용할 수 없습니다.", true),
    NETWORK_UNKNOWN_ERROR(1005, "알 수 없는 네트워크 오류가 발생했습니다.", true),

    // 2000번대: 인증 오류
    AUTHENTICATION_FAILED(2001, "인증에 실패했습니다.", false),
    AUTH_FAILED(2001, "인증에 실패했습니다.", false),  // AUTHENTICATION_FAILED와 동일
    INVALID_API_KEY(2002, "유효하지 않은 API 키입니다.", false),
    EXPIRED_CREDENTIALS(2003, "만료된 인증서입니다.", true),
    UNAUTHORIZED(2004, "권한이 없습니다.", false),
    AUTHENTICATION_REQUIRED(2005, "인증이 필요합니다.", false),
    CRUMB_ACQUISITION_FAILED(2006, "CRUMB 토큰 획득에 실패했습니다.", true),

    // 3000번대: Rate Limiting 오류
    RATE_LIMIT_EXCEEDED(3001, "Rate Limit을 초과했습니다.", true),
    RATE_LIMITED(3001, "Rate Limit을 초과했습니다.", true),  // RATE_LIMIT_EXCEEDED와 동일
    RATE_LIMIT_QUOTA_EXCEEDED(3002, "일일 할당량을 초과했습니다.", false),
    TOO_MANY_REQUESTS(3003, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", true),
    THROTTLED(3004, "요청이 제한되었습니다.", true),

    // 4000번대: 데이터 오류
    DATA_NOT_FOUND(4001, "요청한 데이터를 찾을 수 없습니다.", false),
    NOT_FOUND(4001, "요청한 데이터를 찾을 수 없습니다.", false),  // DATA_NOT_FOUND와 동일
    DATA_CORRUPTED(4002, "데이터가 손상되었습니다.", true),
    INVALID_DATA_FORMAT(4003, "데이터 형식이 유효하지 않습니다.", false),
    DATA_RETRIEVAL_ERROR(4004, "데이터 조회 중 오류가 발생했습니다.", true),
    INCONSISTENT_DATA(4005, "데이터 일관성 오류가 발생했습니다.", false),
    EMPTY_RESPONSE(4006, "서버에서 빈 응답을 반환했습니다.", true),
    INVALID_ETF_TYPE(4010, "심볼이 ETF 또는 뮤추얼펀드가 아닙니다.", false),
    INVALID_MACRO_INDICATOR(4011, "유효하지 않은 거시경제 지표입니다.", false),

    // ufc.stock
    STOCK_DATA_NOT_FOUND(4020, "주식 정보를 찾을 수 없습니다.", false),
    ISIN_NOT_FOUND(4021, "ISIN 정보를 찾을 수 없습니다.", false),
    SHARES_DATA_NOT_FOUND(4022, "발행주식수 정보를 찾을 수 없습니다.", false),

    // ufc.funds
    FUND_DATA_NOT_FOUND(4030, "펀드 정보를 찾을 수 없습니다.", false),
    INVALID_FUND_TYPE(4031, "유효하지 않은 펀드 타입입니다.", false),
    INCOMPLETE_FUND_DATA(4032, "펀드 데이터가 불완전합니다.", false),

    // ufc.price
    PRICE_DATA_NOT_FOUND(4040, "가격 정보를 찾을 수 없습니다.", false),
    INVALID_PERIOD_INTERVAL(4041, "유효하지 않은 기간과 간격 조합입니다.", false),
    INVALID_DATE_RANGE(4042, "유효하지 않은 날짜 범위입니다.", false),
    INCOMPLETE_PRICE_DATA(4043, "가격 데이터가 불완전합니다.", false),

    // 5000번대: 파싱 오류
    JSON_PARSING_ERROR(5001, "JSON 파싱 중 오류가 발생했습니다.", false),
    XML_PARSING_ERROR(5002, "XML 파싱 중 오류가 발생했습니다.", false),
    HTML_PARSING_ERROR(5003, "HTML 파싱 중 오류가 발생했습니다.", false),
    CSV_PARSING_ERROR(5004, "CSV 파싱 중 오류가 발생했습니다.", false),
    UNSUPPORTED_CONTENT_TYPE(5005, "지원하지 않는 컨텐츠 타입입니다.", false),
    ENCODING_ERROR(5006, "인코딩 오류가 발생했습니다.", false),
    DATA_PARSING_ERROR(5010, "데이터 파싱 중 오류가 발생했습니다.", false),

    // 6000번대: 파라미터 오류
    INVALID_PARAMETER(6001, "유효하지 않은 파라미터입니다.", false),
    MISSING_REQUIRED_PARAMETER(6002, "필수 파라미터가 누락되었습니다.", false),
    INVALID_PERIOD(6004, "유효하지 않은 기간입니다.", false),
    INVALID_INTERVAL(6005, "유효하지 않은 간격입니다.", false),
    INVALID_SYMBOL(6006, "유효하지 않은 심볼입니다.", false),

    // 7000번대: 서버 오류
    INTERNAL_SERVER_ERROR(7001, "내부 서버 오류가 발생했습니다.", true),
    SOURCE_ERROR(7001, "소스 오류가 발생했습니다.", true),  // INTERNAL_SERVER_ERROR와 동일
    SERVICE_UNAVAILABLE(7002, "서비스를 사용할 수 없습니다.", true),
    SOURCE_UNAVAILABLE(7002, "소스를 사용할 수 없습니다.", true),  // SERVICE_UNAVAILABLE와 동일
    MAINTENANCE_MODE(7003, "서버가 유지보수 중입니다.", true),
    EXTERNAL_API_ERROR(7004, "외부 API 오류가 발생했습니다.", true),
    DATABASE_ERROR(7005, "데이터베이스 오류가 발생했습니다.", true),

    // 9000번대: 기타 오류
    UNKNOWN_ERROR(9001, "알 수 없는 오류가 발생했습니다.", true),
    CONFIGURATION_ERROR(9002, "설정 오류가 발생했습니다.", false),
    ILLEGAL_STATE(9003, "유효하지 않은 상태입니다.", false),
    NOT_IMPLEMENTED(9004, "구현되지 않은 기능입니다.", false),
}

/**
 * ErrorCode의 재시도 가능 여부를 반환합니다.
 *
 * 일반적으로 네트워크, 타임아웃, 서버 오류는 재시도 가능합니다.
 * 인증 오류, 파라미터 오류는 재시도 불가능합니다.
 *
 * @return 재시도 가능하면 true, 불가능하면 false
 */
fun ErrorCode.isRetryable(): Boolean = this.isRetryableFlag
