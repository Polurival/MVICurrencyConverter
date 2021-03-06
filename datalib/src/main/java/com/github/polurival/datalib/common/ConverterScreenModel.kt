package com.github.polurival.datalib.common

/**
 * @author Польщиков Юрий on 2019-07-26
 */
data class ConverterScreenModel(
    val date: String? = null,
    val focusedCurrencyInfo: CurrencyInfo? = null,
    val currenciesMap: LinkedHashMap<String, CurrencyInfo>? = null
)
