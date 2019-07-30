package com.github.polurival.mvicurrencyconverter

import android.view.ViewGroup

typealias ViewBindingInstantiator = (ViewGroup) -> Any
typealias ViewBindingInstantiatorMap = Map<Class<*>, ViewBindingInstantiator>

class ViewBindingFactory(private val instantiatorMap: ViewBindingInstantiatorMap) {

    /**
     * creates a new ViewBinding
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> create(
        key: Class<*>,
        rootView: ViewGroup
    ) = (instantiatorMap.getValue(key))(rootView) as T
}