package com.github.polurival.mvicurrencyconverter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.freeletics.coredux.LogSink
import com.github.polurival.mockfactory.local.CurrenciesInfoStorageMock
import com.github.polurival.mockfactory.remote.CbrfApiFacadeMock
import kotlinx.coroutines.Dispatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * @author Польщиков Юрий on 2019-08-04
 */
class MainViewModelTest {

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Test
    fun runTest() {

        val cbrfApi = CbrfApiFacadeMock(CbrfApiFacadeMock.SUCCESS_URL)
        val localStorage = CurrenciesInfoStorageMock()
        val loggers = mutableSetOf<LogSink>()
        val memoryCache = LinkedHashMap<String, Any>()
        val coroutineDispatcher = Dispatchers.Unconfined

        val mainStateMachine = MainStateMachine(cbrfApi, localStorage, loggers, memoryCache)
        val viewModel = MainViewModel(mainStateMachine, coroutineDispatcher)


        viewModel.dispatchAction(Action.LoadCurrenciesInfoAction)
        assertThat(viewModel.state.value.toString(), `is`(MainStateMachine.State.LoadingCurrenciesInfoState.toString()))
    }
}