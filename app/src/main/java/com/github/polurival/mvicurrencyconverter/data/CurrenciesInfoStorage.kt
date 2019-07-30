package com.github.polurival.mvicurrencyconverter.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.room.Room
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import io.reactivex.Completable
import io.reactivex.Observable

/**
 * @author Польщиков Юрий on 2019-07-24
 */
class CurrenciesInfoStorage(private val context: Context,
                            private val database: CurrencyInfoDatabase) {

    fun saveDate(date: String) {
        context.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE).edit()
            .putString(PREF_DATE_KEY, date)
            .apply()
    }

    fun loadDate(): String {
        return context.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE).getString(PREF_DATE_KEY, "")!!
    }

    fun saveCurrencies(currenciesInfo: List<CurrencyInfo>) {
        database.currencyInfoDao().insert(currenciesInfo)
    }

    fun loadCurrencies(selectedCurrencies: List<String>): List<CurrencyInfo> {
        return database.currencyInfoDao().getSelectedCurrencies(selectedCurrencies)
    }

    companion object {
        val PREF_FILE_NAME = "miniCurrencyConverter"
        val PREF_DATE_KEY = "prefDateKey"

        @Volatile
        private var INSTANCE: CurrenciesInfoStorage? = null

        fun getStorage(context: Context): CurrenciesInfoStorage {
            return INSTANCE ?: synchronized(this) {
                // Create database here
                val instance = CurrenciesInfoStorage(context.applicationContext, CurrencyInfoDatabase.getDatabase(context))
                INSTANCE = instance
                instance
            }
        }
    }
}