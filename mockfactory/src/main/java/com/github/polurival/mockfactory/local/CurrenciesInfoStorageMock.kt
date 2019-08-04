package com.github.polurival.mockfactory.local

import com.github.polurival.datalib.common.CurrencyInfo
import com.github.polurival.datalib.local.LocalStorage

/**
 * @author Польщиков Юрий on 2019-08-04
 */
class CurrenciesInfoStorageMock : LocalStorage {

    override fun saveDate(date: String) {
        // do nothing
    }

    override fun loadDate(): String {
        return "4.08.2019"
    }

    override fun saveCurrencies(currenciesInfo: List<CurrencyInfo>) {
        // do nothing
    }

    override fun loadCurrencies(selectedCurrencies: List<String>): List<CurrencyInfo> {
        return listOf(
            CurrencyInfo("RUB", 1, "Российский рубль", "1"),
            CurrencyInfo("USD", 1, "Доллар США", "64.6423"),
            CurrencyInfo("EUR", 1, "Евро", "71.7077"),
            CurrencyInfo("CNY", 10, "Китайских юаней", "93.1351")
        )
    }
}