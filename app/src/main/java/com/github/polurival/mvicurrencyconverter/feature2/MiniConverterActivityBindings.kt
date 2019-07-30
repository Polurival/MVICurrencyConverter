package com.github.polurival.mvicurrencyconverter.feature2

import com.badoo.mvicore.android.AndroidBindings
import com.badoo.mvicore.binder.named
import com.badoo.mvicore.binder.using

/**
 * @author Польщиков Юрий on 2019-07-22
 */
class MiniConverterActivityBindings(
    view: MiniConverterActivity,
    private val feature2: Feature2,
    private val newsListener: NewsListener
) : AndroidBindings<MiniConverterActivity>(view) {

    override fun setup(view: MiniConverterActivity) {
        binder.bind(feature2 to view using ViewModelTransformer() named "MiniConverterActivity.ViewModels")
        binder.bind(view to feature2 using UiEventTransformer2())
        binder.bind(feature2.news to newsListener named "MiniConverterActivity.News")
    }
}