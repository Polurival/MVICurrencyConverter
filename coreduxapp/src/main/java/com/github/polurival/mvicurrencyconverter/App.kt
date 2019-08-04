package com.github.polurival.mvicurrencyconverter

import android.app.Application
import android.view.ViewGroup
import com.github.polurival.mvicurrencyconverter.di.ApplicationComponent
import com.github.polurival.mvicurrencyconverter.di.ApplicationModule
import com.github.polurival.mvicurrencyconverter.di.DaggerApplicationComponent
import kotlinx.coroutines.Dispatchers

class App : Application() {

    val applicationComponent: ApplicationComponent by lazy {
        DaggerApplicationComponent.builder().apply {
            componentBuilder(this)
        }.build()
    }

    private fun componentBuilder(builder: DaggerApplicationComponent.Builder): DaggerApplicationComponent.Builder =
        builder.applicationModule(
            ApplicationModule(
                this,
                androidScheduler = Dispatchers.Main,
                viewBindingInstantiatorMap = mapOf<Class<*>,
                        ViewBindingInstantiator>(
                    MainActivity::class.java to { rootView: ViewGroup ->
                        MainViewBinding(
                            rootView
                        )
                    }
                )
            )
        )
}