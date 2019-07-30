package com.github.polurival.mvicurrencyconverter.feature2

import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import java.util.*

/**
 * todo изменение выбранных валют сделать через данный класс.
 * Хранить выбранные валюты в SharedPreferences.
 * Требует контекста, провайдить через di.
 * Переделать хардкод кодов в енум.
 * Строки доставать из ресурсов
 *
 * @author Польщиков Юрий on 2019-07-22
 */
class CurrencySelectionSaver {

    companion object {
        fun getSelectedCurrencies(): TreeMap<String, CurrencyInfo?> {
            val map = TreeMap<String, CurrencyInfo?>()
            map.put("RUB", CurrencyInfo("RUB", 100, "Российский рубль", "100"))
            map.put("USD", null)
            map.put("CNY", null)
            map.put("EUR", null)
            return map
        }
    }
}