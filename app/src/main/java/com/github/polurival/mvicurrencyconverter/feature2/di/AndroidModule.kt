package com.github.polurival.mvicurrencyconverter.feature2.di

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import com.github.polurival.mvicurrencyconverter.App
import dagger.Module
import dagger.Provides

/**
 * @author Польщиков Юрий on 2019-07-22
 */
@Module
class AndroidModule(context: Context) {
    private val context: Context = context.applicationContext

    @Provides
    fun provideApp(): App {
        return context as App
    }

    @Provides
    fun provideContext(): Context {
        return context
    }

    @Provides
    fun provideResources(): Resources {
        return context.resources
    }

    @Provides
    fun provideSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences(App::class.java.name, Context.MODE_PRIVATE)
    }
}