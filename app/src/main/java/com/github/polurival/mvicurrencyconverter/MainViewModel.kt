package com.github.polurival.mvicurrencyconverter

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.github.polurival.mvicurrencyconverter.di.AndroidScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * @author Польщиков Юрий on 2019-07-07
 */
class MainViewModel(@AndroidScheduler private val androidScheduler: CoroutineDispatcher): ViewModel(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext get() = Job() + androidScheduler

    private val mutableState = MutableLiveData<MainStateMachine.State>()
}