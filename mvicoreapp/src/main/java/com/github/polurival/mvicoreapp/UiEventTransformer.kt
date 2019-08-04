package com.github.polurival.mvicoreapp

import com.github.polurival.mvicoreloadcurrenciesfeature.LoadCurrenciesFeature

/**
 * @author Польщиков Юрий on 2019-07-24
 */
class UiEventTransformer : (UiEvent) -> LoadCurrenciesFeature.Wish? {

    override fun invoke(event: UiEvent): LoadCurrenciesFeature.Wish? = when (event) {
        is UiEvent.ScreenOpened -> LoadCurrenciesFeature.Wish.LoadCurrenciesInfo
        is UiEvent.ValueChanged -> null
    }
}