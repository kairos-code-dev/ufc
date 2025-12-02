package com.ulalax.ufc.utils

/**
 * 라이브 테스트용 금융 기호 및 API 시리즈 정의
 *
 * 이 object는 테스트에서 사용할 표준화된 금융 기호들을 정의합니다.
 * 각 기호는 실제 데이터를 조회할 수 있는 유명한 자산들입니다.
 *
 * 사용 예시:
 * ```
 * val stocks = TestSymbols.Stocks.entries.map { it.symbol }
 * val etfs = TestSymbols.ETFs.entries.map { it.symbol }
 * ```
 */
object TestSymbols {

    // ========================================
    // ETF (Exchange-Traded Funds)
    // ========================================

    /**
     * 라이브 테스트용 주요 ETFs
     *
     * 각 ETF는 서로 다른 자산군을 대표하며, 광범위한 테스트 커버리지를 제공합니다.
     */
    enum class ETFs(val symbol: String, val description: String) {
        // 미국 주식 지수 ETF
        SPY("SPY", "S&P 500 ETF - 미국 대형 중형주 지수를 추적합니다"),
        QQQ("QQQ", "Nasdaq 100 ETF - 기술주 중심의 나스닥 100 지수를 추적합니다"),
        IWM("IWM", "Russell 2000 ETF - 미국 소형주 지수를 추적합니다"),
        VTI("VTI", "Vanguard Total Market ETF - 미국 전체 주식 시장을 추적합니다"),

        // 채권 ETF
        AGG("AGG", "Bloomberg Aggregate Bond ETF - 미국 채권 시장을 추적합니다"),
    }

    // ========================================
    // STOCKS (Individual Stocks)
    // ========================================

    /**
     * 라이브 테스트용 주요 개별 주식
     *
     * 대형 기술주와 우량주를 선정하여 다양한 산업 섹터를 커버합니다.
     */
    enum class Stocks(val symbol: String, val description: String) {
        AAPL("AAPL", "Apple Inc. - 세계 최대 기술 기업, 높은 유동성"),
        MSFT("MSFT", "Microsoft Corp. - 클라우드 및 소프트웨어 선도 기업"),
        GOOGL("GOOGL", "Alphabet Inc. - 검색 엔진과 광고 플랫폼의 최강자"),
        AMZN("AMZN", "Amazon.com Inc. - 전자상거래 및 클라우드 서비스 선도 기업"),
        NVDA("NVDA", "NVIDIA Corp. - 반도체 및 AI 칩 설계의 선도자"),
    }

    // ========================================
    // FRED Series IDs (Federal Reserve Economic Data)
    // ========================================

    /**
     * 라이브 테스트용 FRED 경제 지표 시리즈
     *
     * Federal Reserve가 제공하는 주요 거시경제 지표들입니다.
     * 데이터는 월간 또는 분기 단위로 업데이트됩니다.
     */
    enum class FredSeriesIds(val seriesId: String, val description: String) {
        // GDP 지표
        GDPC1("GDPC1", "Real Gross Domestic Product (실질 GDP) - 분기 단위, 미국 경제 규모"),

        // 고용 지표
        UNRATE("UNRATE", "Unemployment Rate (실업률) - 월간 단위, 미국 노동시장의 건강도 지표"),

        // 물가 지표
        CPIAUCSL("CPIAUCSL", "Consumer Price Index All Urban Consumers (CPI) - 월간 단위, 소비자 물가 지수"),

        // 금리 지표
        DFF("DFF", "Effective Federal Funds Rate (연방기금 유효율) - 일간 단위, FED의 기준금리"),
        DGS10("DGS10", "10-Year Treasury Constant Maturity Rate (10년물 국채 수익률) - 일간 단위, 장기 금리 지표"),
    }

    // ========================================
    // Utility Functions
    // ========================================

    /**
     * 모든 ETF 기호를 리스트로 반환합니다.
     *
     * @return ETF 기호 문자열 리스트
     */
    fun getAllETFSymbols(): List<String> = ETFs.entries.map { it.symbol }

    /**
     * 모든 주식 기호를 리스트로 반환합니다.
     *
     * @return 주식 기호 문자열 리스트
     */
    fun getAllStockSymbols(): List<String> = Stocks.entries.map { it.symbol }

    /**
     * 모든 FRED 시리즈 ID를 리스트로 반환합니다.
     *
     * @return FRED 시리즈 ID 문자열 리스트
     */
    fun getAllFredSeriesIds(): List<String> = FredSeriesIds.entries.map { it.seriesId }

    /**
     * 모든 기호 (ETF + 주식)를 리스트로 반환합니다.
     *
     * @return 모든 금융 기호 문자열 리스트
     */
    fun getAllSymbols(): List<String> = getAllETFSymbols() + getAllStockSymbols()
}
