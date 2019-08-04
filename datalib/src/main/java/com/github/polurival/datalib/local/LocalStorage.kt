package com.github.polurival.datalib.local

import com.github.polurival.datalib.common.CurrencyInfo

/**
 * @author Польщиков Юрий on 2019-08-04
 */
interface LocalStorage {

    fun saveDate(date: String)

    fun loadDate(): String

    fun saveCurrencies(currenciesInfo: List<CurrencyInfo>)

    fun loadCurrencies(selectedCurrencies: List<String>): List<CurrencyInfo>
}