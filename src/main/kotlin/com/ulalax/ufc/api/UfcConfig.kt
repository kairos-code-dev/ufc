package com.ulalax.ufc.api

import com.ulalax.ufc.infrastructure.yahoo.YahooClientConfig
import com.ulalax.ufc.infrastructure.fred.FredClientConfig
import com.ulalax.ufc.infrastructure.businessinsider.BusinessInsiderClientConfig

data class UfcConfig(
    val fredApiKey: String? = null,
    val yahooConfig: YahooClientConfig = YahooClientConfig(),
    val fredConfig: FredClientConfig = FredClientConfig(),
    val businessInsiderConfig: BusinessInsiderClientConfig = BusinessInsiderClientConfig()
)
