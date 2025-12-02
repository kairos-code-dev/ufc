package com.ulalax.ufc

import org.slf4j.LoggerFactory

/**
 * UFC 데이터 수집 애플리케이션의 진입점입니다.
 *
 * 이 애플리케이션은 다양한 금융 데이터 소스에서 데이터를 수집하고
 * 통합하여 제공하는 백엔드 서비스입니다.
 */
object App {
    private val logger = LoggerFactory.getLogger(App::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("UFC 데이터 수집 애플리케이션 시작")
        logger.info("버전: 1.0.0")
        logger.info("환경: ${System.getProperty("app.env", "development")}")

        try {
            logger.info("애플리케이션 초기화 완료")
            // Phase 1에서 구현될 코드
        } catch (e: Exception) {
            logger.error("애플리케이션 초기화 실패", e)
            System.exit(1)
        }
    }
}
