package com.github.polurival.mvicurrencyconverter.feature2

import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo

/**
 * @author Польщиков Юрий on 2019-07-22
 */
data class ViewModel(
    /**
     * Дата последнего обновления валют из сети
     */
    val date: String?,
    /**
     * Список информации по валютам
     */
    val currenciesInfo: List<CurrencyInfo>?,
    /**
     * true, если выполняется загрузка
     */
    val currenciesIdLoading: Boolean
)