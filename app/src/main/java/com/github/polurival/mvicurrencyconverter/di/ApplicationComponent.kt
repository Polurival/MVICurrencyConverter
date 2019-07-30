package com.github.polurival.mvicurrencyconverter.di

import com.github.polurival.mvicurrencyconverter.MainActivity
import com.github.polurival.mvicurrencyconverter.ViewBindingFactory
import com.github.polurival.mvicurrencyconverter.cbrf.CbrfApi
import dagger.Component
import javax.inject.Singleton

/**
 * @author Польщиков Юрий on 2019-07-06
 */
@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        LogSinksModule::class
    ]
)
interface ApplicationComponent {

    fun inject(into: MainActivity)
}