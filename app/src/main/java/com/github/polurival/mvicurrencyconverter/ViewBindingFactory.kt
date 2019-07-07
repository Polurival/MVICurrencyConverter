package com.github.polurival.mvicurrencyconverter

import android.view.ViewGroup

/**
 * @author Польщиков Юрий on 2019-07-06
 */
class ViewBindingFactory(private val instantiatorMap: Map<Class<*>, (ViewGroup) -> Any>) {

    @Suppress("UNCHECKED_CAST")
    fun <T> create(key: Class<*>, rootView: ViewGroup) = (instantiatorMap.getValue(key))(rootView) as T
}