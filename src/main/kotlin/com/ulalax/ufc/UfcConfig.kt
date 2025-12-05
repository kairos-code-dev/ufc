package com.ulalax.ufc

import com.ulalax.ufc.yahoo.YahooClientConfig
import com.ulalax.ufc.fred.FredClientConfig
import com.ulalax.ufc.businessinsider.BusinessInsiderClientConfig

data class UfcConfig(
    val fredApiKey: String? = null,
    val yahooConfig: YahooClientConfig = YahooClientConfig(),
    val fredConfig: FredClientConfig = FredClientConfig(),
    val businessInsiderConfig: BusinessInsiderClientConfig = BusinessInsiderClientConfig()
)
