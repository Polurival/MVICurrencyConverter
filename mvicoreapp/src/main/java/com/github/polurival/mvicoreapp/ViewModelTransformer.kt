package com.github.polurival.mvicoreapp

import com.github.polurival.mvicoreloadcurrenciesfeature.LoadCurrenciesFeature

/**
 * @author Польщиков Юрий on 2019-07-22
 */
class ViewModelTransformer : (LoadCurrenciesFeature.State) -> ViewModel {

    override fun invoke(state: LoadCurrenciesFeature.State): ViewModel {
        return ViewModel(
            currenciesIdLoading = state.isLoading,
            date = state.date,
            currenciesInfo = state.currenciesInfo
        )
    }
}