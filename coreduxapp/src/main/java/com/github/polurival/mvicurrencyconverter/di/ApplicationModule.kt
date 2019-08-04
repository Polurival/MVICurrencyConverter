package com.github.polurival.mvicurrencyconverter.di

import android.content.Context
import androidx.room.Room
import com.github.polurival.datalib.local.CurrenciesInfoStorage
import com.github.polurival.datalib.local.CurrencyInfoDatabase
import com.github.polurival.datalib.remote.CbrfApi
import com.github.polurival.datalib.remote.ManualCbrfApiFacade
import com.github.polurival.mvicurrencyconverter.ViewBindingFactory
import com.github.polurival.mvicurrencyconverter.ViewBindingInstantiatorMap

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * @author Польщиков Юрий on 2019-07-06
 */
@Module
class ApplicationModule(
    private val context: Context,
    private val viewBindingInstantiatorMap: ViewBindingInstantiatorMap,
    private val androidScheduler: CoroutineDispatcher
) {

    @Provides
    @Singleton
    fun provideCbrfApi(): CbrfApi = ManualCbrfApiFacade()

    @Provides
    @Singleton
    fun provideCurrenciesInfoStorage(): CurrenciesInfoStorage {
        val currencyInfoDatabase = Room.databaseBuilder(
            context,
            CurrencyInfoDatabase::class.java,
            "currencyInfoDatabase"
        ).build()
        return CurrenciesInfoStorage(context, currencyInfoDatabase)
    }

    @Provides
    @Singleton
    fun provideMemoryCache(): LinkedHashMap<String, Any> {
        return LinkedHashMap()
    }

    @Provides
    @Singleton
    fun provideViewBindingFactory() = ViewBindingFactory(viewBindingInstantiatorMap)

    @Provides
    @Singleton
    @AndroidScheduler
    fun provideAndroidScheduler() = androidScheduler
}