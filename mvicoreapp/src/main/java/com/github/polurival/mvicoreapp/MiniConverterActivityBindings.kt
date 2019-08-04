package com.github.polurival.mvicoreapp

import com.badoo.mvicore.android.AndroidBindings
import com.badoo.mvicore.binder.named
import com.badoo.mvicore.binder.using
import com.github.polurival.mvicoreloadcurrenciesfeature.LoadCurrenciesFeature

/**
 * @author Польщиков Юрий on 2019-07-22
 */
class MiniConverterActivityBindings(
    view: MiniConverterActivity,
    private val loadCurrenciesFeature: LoadCurrenciesFeature,
    private val newsListener: NewsListener
) : AndroidBindings<MiniConverterActivity>(view) {

    override fun setup(view: MiniConverterActivity) {
        binder.bind(loadCurrenciesFeature to view using ViewModelTransformer() named "MiniConverterActivity.ViewModels")
        binder.bind(view to loadCurrenciesFeature using UiEventTransformer())
        binder.bind(loadCurrenciesFeature.news to newsListener named "MiniConverterActivity.News")
    }
}