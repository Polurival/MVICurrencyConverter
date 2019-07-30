package com.github.polurival.mvicurrencyconverter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.freeletics.coredux.subscribeToChangedStateUpdates
import com.github.polurival.mvicurrencyconverter.di.AndroidScheduler
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * @author Польщиков Юрий on 2019-07-07
 */
class MainViewModel @Inject constructor(
    mainStateMachine: MainStateMachine,
    @AndroidScheduler private val androidScheduler: CoroutineDispatcher
) : ViewModel(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext get() = androidScheduler + job

    private val mutableState = MutableLiveData<MainStateMachine.State>()

    private val mainStore = mainStateMachine.create(this).also {
        it.subscribeToChangedStateUpdates { newState: MainStateMachine.State ->
            mutableState.value = newState
        }
    }

    val dispatchAction: (Action) -> Unit = mainStore::dispatch
    val state: LiveData<MainStateMachine.State> = mutableState

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}