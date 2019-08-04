package com.github.polurival.mockfactory.remote

import com.github.polurival.datalib.common.CurrencyInfo
import com.github.polurival.datalib.remote.CbrfApi
import com.github.polurival.datalib.remote.CbrfResponse

/**
 * @author Польщиков Юрий on 2019-08-04
 */
class CbrfApiFacadeMock(private val mockUrl: String): CbrfApi {

    override fun loadCurrenciesInfo(): CbrfResponse {
        val response : CbrfResponse

        if (mockUrl == SUCCESS_URL) {
            val currenciesMap = LinkedHashMap<String, CurrencyInfo>()
            currenciesMap["RUB"] = CurrencyInfo("RUB", 1, "Российский рубль", "1")
            currenciesMap["RUB"] = CurrencyInfo("USD", 1, "Доллар США", "64.6423")
            currenciesMap["RUB"] = CurrencyInfo("EUR", 1, "Евро", "71.7077")
            currenciesMap["RUB"] = CurrencyInfo("CNY", 10, "Китайских юаней", "93.1351")
            response = CbrfResponse("4.08.2019", currenciesMap)
        } else {
            throw IllegalArgumentException()
        }

        return response
    }

    companion object {
        const val SUCCESS_URL = "successUrl"
    }
}