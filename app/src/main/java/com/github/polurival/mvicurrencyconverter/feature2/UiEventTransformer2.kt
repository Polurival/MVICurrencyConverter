package com.github.polurival.mvicurrencyconverter.feature2

/**
 * @author Польщиков Юрий on 2019-07-24
 */
class UiEventTransformer2 : (UiEvent) -> Feature2.Wish? {

    override fun invoke(event: UiEvent): Feature2.Wish? = when (event) {
        is UiEvent.ScreenOpened -> Feature2.Wish.LoadCurrenciesInfo
        is UiEvent.ValueChanged -> null
    }
}