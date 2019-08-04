package com.github.polurival.datalib.remote

/**
 * @author Польщиков Юрий on 2019-07-22
 */
interface CbrfApi {

    fun loadCurrenciesInfo() : CbrfResponse
}