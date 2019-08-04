package com.github.polurival.datalib.local

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.github.polurival.datalib.common.CurrencyInfo

/**
 * @author Польщиков Юрий on 2019-07-24
 */
class CurrenciesInfoStorage(
    private val context: Context,
    private val database: CurrencyInfoDatabase
) : LocalStorage {

    override fun saveDate(date: String) {
        context.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE).edit()
            .putString(PREF_DATE_KEY, date)
            .apply()
    }

    override fun loadDate(): String {
        return context.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE).getString(PREF_DATE_KEY, "")!!
    }

    override fun saveCurrencies(currenciesInfo: List<CurrencyInfo>) {
        database.currencyInfoDao().insert(currenciesInfo)
    }

    override fun loadCurrencies(selectedCurrencies: List<String>): List<CurrencyInfo> {
        return database.currencyInfoDao().getSelectedCurrencies(
            selectedCurrencies[0], selectedCurrencies[1],
            selectedCurrencies[2], selectedCurrencies[3]
        )
    }

    companion object {
        const val PREF_FILE_NAME = "miniCurrencyConverter"
        const val PREF_DATE_KEY = "prefDateKey"
    }
}