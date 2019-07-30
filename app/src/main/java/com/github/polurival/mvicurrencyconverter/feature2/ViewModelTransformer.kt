package com.github.polurival.mvicurrencyconverter.feature2

/**
 * @author Польщиков Юрий on 2019-07-22
 */
class ViewModelTransformer : (Feature2.State) -> ViewModel {

    override fun invoke(state: Feature2.State): ViewModel {
        return ViewModel(
            currenciesIdLoading = state.isLoading,
            date = state.date,
            currenciesInfo = state.currenciesInfo
        )
    }
}