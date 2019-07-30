package com.github.polurival.mvicurrencyconverter.cbrf

import com.github.polurival.mvicurrencyconverter.dto.CbrfResponse

/**
 * @author Польщиков Юрий on 2019-07-22
 */
interface CbrfApi {

    fun loadCurrenciesInfo() : CbrfResponse
}