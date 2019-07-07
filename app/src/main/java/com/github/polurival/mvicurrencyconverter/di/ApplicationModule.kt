package com.github.polurival.mvicurrencyconverter.di

import android.view.ViewGroup
import com.github.polurival.mvicurrencyconverter.ViewBindingFactory
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * @author Польщиков Юрий on 2019-07-06
 */
@Module
class ApplicationModule(
    private val baseUrl: String,
    private val viewBindingInstantiatorMap: Map<Class<*>, (ViewGroup) -> Any>,
    private val androidScheduler: CoroutineDispatcher
) {

    @Provides
    @Singleton
    fun provideViewBindingFactory() = ViewBindingFactory(viewBindingInstantiatorMap)

    @Provides
    @Singleton
    @AndroidScheduler
    fun provideAndroidScheduler() = androidScheduler
}