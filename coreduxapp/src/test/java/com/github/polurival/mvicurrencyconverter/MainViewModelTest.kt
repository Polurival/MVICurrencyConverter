package com.github.polurival.mvicurrencyconverter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.freeletics.coredux.LogSink
import com.github.polurival.datalib.common.ConverterScreenModel
import com.github.polurival.datalib.common.CurrencyInfo
import com.github.polurival.datalib.local.LocalStorage
import com.github.polurival.mockfactory.local.CurrenciesInfoStorageMock
import com.github.polurival.mockfactory.remote.CbrfApiFacadeMock
import kotlinx.coroutines.Dispatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * @author Польщиков Юрий on 2019-08-04
 */
class MainViewModelTest {

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    lateinit var localStorage: LocalStorage;

    @Before
    fun setUp() {
        localStorage = CurrenciesInfoStorageMock();
    }

    @Test
    fun runTest() {

        val cbrfApi = CbrfApiFacadeMock(CbrfApiFacadeMock.SUCCESS_URL)
        val loggers = mutableSetOf<LogSink>()
        val memoryCache = LinkedHashMap<String, Any>()
        val coroutineDispatcher = Dispatchers.Unconfined

        val mainStateMachine = MainStateMachine(cbrfApi, localStorage, loggers, memoryCache)
        val viewModel = MainViewModel(mainStateMachine, coroutineDispatcher)

        // initial state
        assertThat(viewModel.state.value.toString(), `is`(MainStateMachine.State.LoadingCurrenciesInfoState.toString()))

        viewModel.dispatchAction(Action.LoadCurrenciesInfoAction)
        assertThat(viewModel.state.value.toString(), `is`(MainStateMachine.State.LoadingCurrenciesInfoState.toString()))

        val focusedCurrency = CurrencyInfo("RUB", 1, "Российский рубль", "1.0")
        viewModel.dispatchAction(SaveCbrfApiResponseAction(cbrfApi.loadCurrenciesInfo(), focusedCurrency))
        assertThat(viewModel.state.value.toString(), `is`(MainStateMachine.State.LoadingCurrenciesInfoState.toString()))

        viewModel.dispatchAction(PrepareCurrenciesToShowAction(ConverterScreenModel(focusedCurrencyInfo = focusedCurrency)))
        assertThat(viewModel.state.value.toString(), `is`(MainStateMachine.State.LoadingCurrenciesInfoState.toString()))

        val screenModel = ConverterScreenModel(localStorage.loadDate(), focusedCurrency, getCurrenciesInfoMap())
        viewModel.dispatchAction(ShowCurrenciesInfoAction(screenModel))
        assertThat(
            viewModel.state.value.toString(),
            `is`(MainStateMachine.State.ShowCurrenciesInfoState(screenModel).toString())
        )
    }

    private fun getCurrenciesInfoMap(): LinkedHashMap<String, CurrencyInfo> {
        val map = LinkedHashMap<String, CurrencyInfo>();
        val currenciesInfoList = localStorage.loadCurrencies(emptyList())
        for (item in currenciesInfoList) {
            map[item.charCode] = CurrencyInfo(item.charCode, item.nominal, item.name, item.value)
        }
        return map
    }
}