package com.ulalax.ufc.infrastructure.businessinsider.internal

import com.ulalax.ufc.infrastructure.businessinsider.BusinessInsiderClientConfig
import com.ulalax.ufc.domain.model.security.IsinSearchResult
import com.ulalax.ufc.domain.exception.ApiException
import com.ulalax.ufc.domain.exception.DataParsingException
import com.ulalax.ufc.domain.exception.ErrorCode
import com.ulalax.ufc.domain.exception.ValidationException
import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimiter
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

/**
 * Business Insider API HTTP 클라이언트
 *
 * ISIN 기반 검색을 통해 심볼과 종목 정보를 조회합니다.
 *
 * @property config 클라이언트 설정
 * @property rateLimiter Rate Limiter
 */
internal class BusinessInsiderHttpClient(
    private val config: BusinessInsiderClientConfig,
    private val rateLimiter: RateLimiter
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(BusinessInsiderHttpClient::class.java)

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = config.connectTimeoutMs
            requestTimeoutMillis = config.requestTimeoutMs
        }

        if (config.enableLogging) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }

        defaultRequest {
            headers {
                append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                append(HttpHeaders.Accept, "*/*")
            }
        }
    }

    /**
     * ISIN으로 종목 검색
     *
     * @param isin 국제증권식별번호 (12자리)
     * @return ISIN 검색 결과
     * @throws ValidationException ISIN 형식이 잘못된 경우
     * @throws ApiException 네트워크 오류 또는 파싱 실패
     */
    suspend fun searchByIsin(isin: String): IsinSearchResult {
        // 대소문자 정규화 (ISIN은 대문자)
        val normalizedIsin = isin.uppercase()
        validateIsin(normalizedIsin)

        val url = BusinessInsiderApiUrls.searchUrl(normalizedIsin)
        logger.debug("Searching ISIN: {} at {}", normalizedIsin, url)

        // Rate Limit 적용
        rateLimiter.acquire()

        return try {
            // JavaScript callback 형식 응답 받기
            val responseText: String = httpClient.get(url).body()
            logger.debug("Raw response: {}", responseText.take(200))

            // 응답 파싱
            val results = parseJsCallback(responseText)

            // ISIN과 정확히 일치하는 결과 찾기 또는 첫 번째 결과 사용
            val matchingResult = results.find { it.isin.equals(normalizedIsin, ignoreCase = true) }
                ?: results.firstOrNull()
                ?: throw ApiException(
                    errorCode = ErrorCode.DATA_NOT_FOUND,
                    message = "No results found for ISIN: $normalizedIsin",
                    metadata = mapOf("isin" to normalizedIsin)
                )

            IsinSearchResult(
                isin = matchingResult.isin.ifEmpty { normalizedIsin },
                symbol = matchingResult.symbol.ifEmpty {
                    throw DataParsingException(
                        errorCode = ErrorCode.DATA_PARSING_ERROR,
                        message = "Symbol not found for ISIN: $normalizedIsin",
                        metadata = mapOf("isin" to normalizedIsin)
                    )
                },
                name = matchingResult.name,
                exchange = null,
                currency = null,
                type = matchingResult.category
            )
        } catch (e: ValidationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: DataParsingException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to search ISIN: {}", normalizedIsin, e)
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Failed to search ISIN $normalizedIsin: ${e.message}",
                cause = e,
                metadata = mapOf("isin" to normalizedIsin)
            )
        }
    }

    /**
     * JavaScript callback 응답 파싱
     *
     * 응답 형식:
     * mmSuggestDeliver(0, new Array("Name", "Category", "Keywords", ...), new Array(
     *   new Array("Apple Inc.", "Stocks", "AAPL|US0378331005|AAPL||AAPL", ...),
     *   ...
     * ), count, 0);
     *
     * Keywords 필드 형식: "SYMBOL|ISIN|SYMBOL||SYMBOL" 또는 "|ISIN|||"
     */
    private fun parseJsCallback(response: String): List<ParsedResult> {
        val results = mutableListOf<ParsedResult>()

        // new Array("Name", "Value", ...) 패턴 매칭
        val arrayPattern = Regex("""new Array\("([^"]*)"\s*,\s*"([^"]*)"\s*,\s*"([^"]*)"""")
        val matches = arrayPattern.findAll(response)

        for (match in matches) {
            val name = match.groupValues[1]
            val category = match.groupValues[2]
            val keywords = match.groupValues[3]

            // 헤더 행 건너뛰기 (name이 "Name"인 경우)
            if (name == "Name" && category == "Category" && keywords == "Keywords") {
                continue
            }

            // Keywords 파싱: "SYMBOL|ISIN|..." 형식
            val parts = keywords.split("|")
            val symbol = parts.getOrNull(0) ?: ""
            val isin = parts.getOrNull(1) ?: ""

            // 유효한 결과만 추가
            if (name.isNotEmpty() && (symbol.isNotEmpty() || isin.isNotEmpty())) {
                results.add(ParsedResult(name, category, symbol, isin))
            }
        }

        return results
    }

    private data class ParsedResult(
        val name: String,
        val category: String,
        val symbol: String,
        val isin: String
    )

    /**
     * ISIN 형식 검증
     *
     * ISIN은 12자리 영숫자로 구성됩니다:
     * - 국가 코드 (2자리): ISO 3166-1 alpha-2
     * - 식별자 (9자리): 숫자 또는 문자
     * - 체크섬 (1자리): 숫자
     */
    private fun validateIsin(isin: String) {
        if (isin.length != 12) {
            throw ValidationException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "ISIN must be 12 characters: $isin",
                field = "isin",
                metadata = mapOf("isin" to isin, "length" to isin.length)
            )
        }
        if (!isin.matches(Regex("^[A-Z]{2}[A-Z0-9]{9}[0-9]$"))) {
            throw ValidationException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "Invalid ISIN format: $isin",
                field = "isin",
                metadata = mapOf("isin" to isin, "expectedFormat" to "^[A-Z]{2}[A-Z0-9]{9}[0-9]$")
            )
        }
    }

    override fun close() {
        httpClient.close()
    }
}
