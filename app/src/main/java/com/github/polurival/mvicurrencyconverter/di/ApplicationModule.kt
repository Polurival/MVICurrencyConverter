package com.github.polurival.mvicurrencyconverter.di

import android.content.Context
import androidx.room.Room
import com.github.polurival.mvicurrencyconverter.ViewBindingFactory
import com.github.polurival.mvicurrencyconverter.ViewBindingInstantiatorMap
import com.github.polurival.mvicurrencyconverter.cbrf.CbrfApi
import com.github.polurival.mvicurrencyconverter.cbrf.ManualCbrfApiFacade
import com.github.polurival.mvicurrencyconverter.data.CurrenciesInfoStorage
import com.github.polurival.mvicurrencyconverter.data.CurrencyInfoDatabase
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * @author Польщиков Юрий on 2019-07-06
 */
@Module
class ApplicationModule(
    private val context: Context,
    //private val viewBindingInstantiatorMap: ViewBindingInstantiatorMap,
    private val androidScheduler: CoroutineDispatcher
) {

    @Provides
    @Singleton
    fun provideCbrfApi(): CbrfApi = ManualCbrfApiFacade()

    @Provides
    @Singleton
    fun provideCurrencyInfoDatabase(): CurrencyInfoDatabase {
        return Room.databaseBuilder(
            context,
            CurrencyInfoDatabase::class.java,
            "currencyInfoDatabase"
        ).build()
    }

    @Provides
    @Singleton
    fun provideCurrenciesInfoStorage(currencyInfoDatabase: CurrencyInfoDatabase): CurrenciesInfoStorage {
        return CurrenciesInfoStorage(context, currencyInfoDatabase)
    }

    @Provides
    @Singleton
    fun provideMemoryCache(): LinkedHashMap<String, Any> {
        return LinkedHashMap()
    }

    /*@Provides
    @Singleton
    fun provideViewBindingFactory() = ViewBindingFactory(viewBindingInstantiatorMap)*/

    @Provides
    @Singleton
    @AndroidScheduler
    fun provideAndroidScheduler() = androidScheduler
}