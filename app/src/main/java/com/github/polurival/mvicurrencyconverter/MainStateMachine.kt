package com.github.polurival.mvicurrencyconverter

import com.freeletics.coredux.*
import com.github.polurival.mvicurrencyconverter.cbrf.CbrfApi
import com.github.polurival.mvicurrencyconverter.data.CurrenciesInfoStorage
import com.github.polurival.mvicurrencyconverter.dto.CbrfResponse
import com.github.polurival.mvicurrencyconverter.dto.ConverterScreenModel
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

/**
 * @author Польщиков Юрий on 2019-07-06
 */
sealed class Action {

    object LoadCurrenciesInfoAction : Action() {
        override fun toString(): String = LoadCurrenciesInfoAction::class.java.simpleName
    }

    data class UpdateCurrenciesInfoAction(val screenModel: ConverterScreenModel) : Action() {
        override fun toString(): String = "${UpdateCurrenciesInfoAction::class.java.simpleName} " +
                "+ focusedCurrency=${screenModel.focusedCurrencyInfo?.charCode} " +
                "+ currenciesCount=${screenModel.currenciesMap?.size}"
    }

    data class CalculateNewValuesAction(val changedCurrency: CurrencyInfo) : Action() {
        override fun toString(): String = "${CalculateNewValuesAction::class.java.simpleName} " +
                "+ changedCurrency=${changedCurrency.charCode} " +
                "+ newValue=${changedCurrency.value}"
    }
}

private object StartLoadingCurrenciesInfoAction : Action() {
    override fun toString(): String = StartLoadingCurrenciesInfoAction::class.java.simpleName
}

private data class ErrorLoadingCurrenciesInfoAction(val error: Throwable) : Action() {
    override fun toString(): String =
        "${ErrorLoadingCurrenciesInfoAction::class.java.simpleName} + error=$error"
}

private data class SaveCbrfApiResponseAction(val response: CbrfResponse,
                                             val focusedCurrency: CurrencyInfo?) : Action() {
    override fun toString(): String =
        "${SaveCbrfApiResponseAction::class.java.simpleName} + responseDate=${response.date} " +
                "+ currenciesCount=${response.currenciesInfo.size} " +
                "+ focusedCurrency=${focusedCurrency?.charCode}"
}

private data class PrepareCurrenciesToShowAction(val screenModel: ConverterScreenModel?) : Action() {
    override fun toString(): String = "${PrepareCurrenciesToShowAction::class.java.simpleName} " +
            "+ focusedCurrency=${screenModel?.focusedCurrencyInfo?.charCode} " +
            "+ currenciesCount=${screenModel?.currenciesMap?.size}"
}

private data class ShowCurrenciesInfoAction(val screenModel: ConverterScreenModel) : Action() {
    override fun toString(): String = "${ShowCurrenciesInfoAction::class.java.simpleName} " +
            "+ loadDate=${screenModel.date} " +
            "+ currenciesCount=${screenModel.currenciesMap?.size}"
}

private data class ShowErrorUpdatingCurrenciesInfoAction(val error: Throwable) : Action() {
    override fun toString(): String =
        "${ShowErrorUpdatingCurrenciesInfoAction::class.java.simpleName} + error=$error"
}

private data class HideErrorUpdatingCurrenciesInfoAction(val error: Throwable) : Action() {
    override fun toString(): String =
        "${HideErrorUpdatingCurrenciesInfoAction::class.java.simpleName} + error=$error"
}

class MainStateMachine @Inject constructor(
    private val cbrfApi: CbrfApi,
    private val currencyInfoStorage: CurrenciesInfoStorage,
    private val loggers: MutableSet<LogSink>,
    private val memoryCash: LinkedHashMap<String, Any>
) {

    private interface ContainsItems {
        val screenModel: ConverterScreenModel
    }

    sealed class State {

        /**
         * Состояние загрузки, когда отображается только прогресс
         */
        object LoadingCurrenciesInfoState : State() {
            override fun toString(): String = LoadingCurrenciesInfoState::class.java.simpleName
        }

        /**
         * Состояние ошибки, когда данных еще нет и произошла ошибка их загрузки
         */
        data class ErrorLoadingCurrenciesInfoState(val errorMessage: String) : State() {
            override fun toString(): String =
                "${ErrorLoadingCurrenciesInfoState::class.java.simpleName} error=$errorMessage"
        }

        /**
         * Состояние при котором показаны данные
         */
        data class ShowCurrenciesInfoState(override val screenModel: ConverterScreenModel) : State(), ContainsItems {
            override fun toString(): String =
                "${ShowCurrenciesInfoState::class.java.simpleName} + loadDate=${screenModel.date} " +
                        "+ currenciesCount=${screenModel.currenciesMap?.size}"
        }

        /**
         * Состояние при котором показаны данные и идет загрузка их обновления
         */
        data class ShowCurrenciesAndUpdateState(override val screenModel: ConverterScreenModel) : State(),
            ContainsItems {
            override fun toString(): String =
                "${ShowCurrenciesAndUpdateState::class.java.simpleName} + loadDate=${screenModel.date} " +
                        "+ currenciesCount=${screenModel.currenciesMap?.size}"
        }

        /**
         * Состояние при котором показаны данные и показана ошибка их обновления
         */
        data class ShowCurrenciesInfoAndUpdateErrorState(
            override val screenModel: ConverterScreenModel,
            val errorMessage: String
        ) : State(), ContainsItems {
            override fun toString(): String =
                "${ShowCurrenciesInfoAndUpdateErrorState::class.java.simpleName} + loadDate=${screenModel.date} " +
                        "currenciesCount=${screenModel.currenciesMap?.size} error=$errorMessage"
        }
    }

    private val loadCurrenciesInfoSideEffect = CancellableSideEffect<State, Action>(
        name = "Load Currencies Info"
    ) { state, action, logger, handler ->
        val currentState = state()
        if (action is Action.LoadCurrenciesInfoAction &&
            currentState !is ContainsItems
            || action is Action.UpdateCurrenciesInfoAction
        ) {
            handler { name, output ->
                logger.logSideEffectEvent { LogEvent.SideEffectEvent.Custom(name, "Start loading currencies info") }
                //val currentScreenModel = (action as? Action.UpdateCurrenciesInfoAction)?.screenModel
                loadCurrenciesInfo(action, output, name, logger)
            }
        } else {
            null
        }
    }

    private val saveCbrfResponseSideEffect = CancellableSideEffect<State, Action>(
        name = "Save Cbrf response"
    ) { state, action, logger, handler ->
        if (action is SaveCbrfApiResponseAction) {
            handler { name, output ->
                logger.logSideEffectEvent { LogEvent.SideEffectEvent.Custom(name, "Start saving Cbrf response") }
                saveCbrfResponse(action, output, name, logger)
            }
        } else {
            null
        }
    }

    private val prepareCurrenciesSideEffect = CancellableSideEffect<State, Action>(
        name = "Prepare currencies to show"
    ) { state, action, logger, handler ->
        if (action is PrepareCurrenciesToShowAction) {
            handler { name, output ->
                logger.logSideEffectEvent {
                    LogEvent.SideEffectEvent.Custom(name, "Start preparing currencies to show")
                }
                prepareCurrenciesToShow(action.screenModel, output, name, logger)
            }
        } else {
            null
        }
    }

    private fun CoroutineScope.prepareCurrenciesToShow(
        currentScreenModel: ConverterScreenModel?,
        output: SendChannel<Action>,
        sideEffectName: String,
        logger: SideEffectLogger
    ): Job = launch {
        try {
            val screenModel: ConverterScreenModel
            if (currentScreenModel?.currenciesMap == null) {
                // если происходит первоначальная загрузка
                screenModel = withContext(Dispatchers.IO) {
                    async {
                        val loadDate = currencyInfoStorage.loadDate()

                        val currenciesInfo = currencyInfoStorage.loadCurrencies(getChosenCurrencies())
                        val currenciesMap = LinkedHashMap<String, CurrencyInfo>()
                        for (currencyInfo in currenciesInfo) {
                            currenciesMap[currencyInfo.charCode] = currencyInfo
                        }
                        saveDataInMemoryCache(loadDate, currenciesMap)

                        ConverterScreenModel(loadDate, currentScreenModel?.focusedCurrencyInfo, currenciesMap)
                    }.await()
                }
            } else {
                screenModel = currentScreenModel
            }
            ShowCurrenciesInfoAction(screenModel)
                .run {
                    logger.logSideEffectEvent {
                        LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, this)
                    }
                    output.send(this)
                }
        } catch (error: Throwable) {
            logger.logSideEffectEvent {
                LogEvent.SideEffectEvent.Custom(sideEffectName, "Error on preparing currencies to show: $error")
            }
            ErrorLoadingCurrenciesInfoAction(error)
                .run {
                    logger.logSideEffectEvent {
                        LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, this)
                    }
                    output.send(this)
                }
        }
    }

    private fun CoroutineScope.saveCbrfResponse(
        action: SaveCbrfApiResponseAction,
        output: SendChannel<Action>,
        sideEffectName: String,
        logger: SideEffectLogger
    ): Job = launch {
        try {
            withContext(Dispatchers.IO) {
                async {
                    currencyInfoStorage.saveDate(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))
                    val currenciesInfo = action.response.currenciesInfo.values.toMutableList()
                    //currenciesInfo.add(0, CurrencyInfo(""))
                    currencyInfoStorage.saveCurrencies(action.response.currenciesInfo.values.toList())
                }.await()
            }
            PrepareCurrenciesToShowAction(ConverterScreenModel(focusedCurrencyInfo = action.focusedCurrency))
                .run {
                    logger.logSideEffectEvent {
                        LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, this)
                    }
                    output.send(this)
                }
        } catch (error: Throwable) {
            logger.logSideEffectEvent {
                LogEvent.SideEffectEvent.Custom(sideEffectName, "Error on saving Cbrf response info: $error")
            }
            ErrorLoadingCurrenciesInfoAction(error)
                .run {
                    logger.logSideEffectEvent {
                        LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, this)
                    }
                    output.send(this)
                }
        }
    }

    /**
     * Loads currencies info from rest service
     */
    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.loadCurrenciesInfo(
        action: Action,
        output: SendChannel<Action>,
        sideEffectName: String,
        logger: SideEffectLogger
    ): Job = launch {

        StartLoadingCurrenciesInfoAction
            .run {
                logger.logSideEffectEvent {
                    LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, this)
                }
                output.send(this)
            }

        delay(TimeUnit.SECONDS.toMillis(1)) // Add some delay to make the loading indicator appear

        try {
            val result = withContext(Dispatchers.IO) {
                async {
                    val todayDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                    val lastLoadDate = currencyInfoStorage.loadDate()

                    if (action is Action.UpdateCurrenciesInfoAction ||
                        (action is Action.LoadCurrenciesInfoAction && todayDate != lastLoadDate)
                    ) {
                        cbrfApi.loadCurrenciesInfo()
                    } else {
                        null
                    }
                }.await()
            }

            val focusedCurrency = if (action is Action.UpdateCurrenciesInfoAction) {
                action.screenModel.focusedCurrencyInfo
            } else {
                CurrencyInfo("RUB", 1, "Российский рубль", "100")
            }
            if (result == null) {
                // если происходит первоначальная загрузка и в базе данных уже есть актуальные данные
                PrepareCurrenciesToShowAction(ConverterScreenModel(focusedCurrencyInfo = focusedCurrency))
                    .run {
                        logger.logSideEffectEvent {
                            LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, this)
                        }
                        output.send(this)
                    }
            } else {
                SaveCbrfApiResponseAction(result, focusedCurrency)
                    .run {
                        logger.logSideEffectEvent {
                            LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, this)
                        }
                        output.send(this)
                    }
            }
        } catch (error: Throwable) {
            logger.logSideEffectEvent {
                LogEvent.SideEffectEvent.Custom(sideEffectName, "Error on loading currencies info: $error")
            }
            ErrorLoadingCurrenciesInfoAction(error)
                .run {
                    logger.logSideEffectEvent {
                        LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, this)
                    }
                    output.send(this)
                }
        }
    }

    /**
     * The state reducer.
     * Takes Actions and the current state to calculate the new state.
     */
    private fun reducer(state: State, action: Action): State {
        return when (action) {
            is StartLoadingCurrenciesInfoAction -> {
                if (state is ContainsItems) {
                    State.ShowCurrenciesAndUpdateState(state.screenModel)
                } else {
                    State.LoadingCurrenciesInfoState
                }
            }
            is ErrorLoadingCurrenciesInfoAction -> {
                State.ErrorLoadingCurrenciesInfoState(action.error.localizedMessage ?: action.error.toString())
            }
            // это действие вызывается когда произошла первоначальная загрузка или обновление
            is ShowCurrenciesInfoAction -> {
                if (state is ContainsItems && action.screenModel.focusedCurrencyInfo != null) {
                    // обновление
                    val preparedCurrencies = LinkedHashMap<String, CurrencyInfo>()

                    val focusedCharCode = action.screenModel.focusedCurrencyInfo.charCode
                    val focusedValue = BigDecimal(action.screenModel.focusedCurrencyInfo.value)
                    val focusedNominal = BigDecimal(action.screenModel.focusedCurrencyInfo.nominal)

                    val date: String = memoryCash["date"] as String
                    for (key in memoryCash.keys) {
                        if (key == "date") {
                            continue
                        }
                        val currencyInfo = memoryCash[key] as CurrencyInfo
                        if (key == focusedCharCode) {
                            preparedCurrencies[key] = action.screenModel.focusedCurrencyInfo
                        } else {
                            val newValue = focusedValue.multiply(BigDecimal(currencyInfo.nominal))
                                .divide(BigDecimal(currencyInfo.value), 2, RoundingMode.HALF_EVEN)
                                .divide(focusedNominal, 2, RoundingMode.HALF_EVEN)
                            preparedCurrencies[key] = currencyInfo.copy(value = newValue.toPlainString())
                        }
                    }

                    /*for (currencyEntry in state.screenModel.currenciesMap!!) {
                        val newCurrencyInfo = action.screenModel.currenciesMap!![currencyEntry.key]
                        if ("RUB" != currencyEntry.key) {
                            val newValue = BigDecimal(newCurrencyInfo!!.nominal).multiply(focusedValue)
                                .divide(BigDecimal(newCurrencyInfo.value), 2, RoundingMode.HALF_EVEN)
                            preparedCurrencies[currencyEntry.key] =
                                currencyEntry.value.copy(value = newValue.toPlainString())
                        } else {
                            preparedCurrencies[currencyEntry.key] = currencyEntry.value
                        }
                    }*/
                    State.ShowCurrenciesInfoState(
                        action.screenModel.copy(
                            date = date,
                            currenciesMap = preparedCurrencies
                        )
                    )
                } else {
                    // первоначальная загрузка
                    val preparedCurrencies = LinkedHashMap<String, CurrencyInfo>()
                    // todo внедрить менеджер ресурсов
                    preparedCurrencies["RUB"] = CurrencyInfo("RUB", 1, "Российский рубль", "100")
                    for (currencyEntry in action.screenModel.currenciesMap!!) {
                        val newValue = BigDecimal(currencyEntry.value.nominal * 100)
                            .divide(BigDecimal(currencyEntry.value.value), 2, RoundingMode.HALF_EVEN)
                        preparedCurrencies[currencyEntry.key] =
                            currencyEntry.value.copy(value = newValue.toPlainString())
                    }
                    State.ShowCurrenciesInfoState(action.screenModel.copy(currenciesMap = preparedCurrencies))
                }
            }
            // это действие вызывается при редактировании значения пользователем
            is Action.CalculateNewValuesAction -> {
                if (state !is ContainsItems) {
                    throw IllegalStateException("We never loaded currencies")
                }

                val preparedCurrencies = LinkedHashMap<String, CurrencyInfo>()

                val focusedCharCode = action.changedCurrency.charCode
                val focusedValue = BigDecimal(action.changedCurrency.value)
                val focusedNominal = BigDecimal(action.changedCurrency.nominal)

                for (key in memoryCash.keys) {
                    if (key == "date") {
                        continue
                    }
                    val currencyInfo = memoryCash[key] as CurrencyInfo
                    if (key == focusedCharCode) {
                        preparedCurrencies[key] = action.changedCurrency
                    } else {
                        val newValue = focusedValue.multiply(BigDecimal(currencyInfo.nominal))
                            .divide(BigDecimal(currencyInfo.value), 2, RoundingMode.HALF_EVEN)
                            .divide(focusedNominal, 2, RoundingMode.HALF_EVEN)
                        preparedCurrencies[key] = currencyInfo.copy(value = newValue.toPlainString())
                    }
                }
                State.ShowCurrenciesInfoState(state.screenModel.copy(currenciesMap = preparedCurrencies))



                // todo здесь тоже брать значения из кэша а не из state (currenciesMap)
                /*val oldChangedValue = state.screenModel
                    .currenciesMap!![action.changedCurrency.charCode]?.value
                if (oldChangedValue == action.changedCurrency.value) {
                    State.ShowCurrenciesInfoState(state.screenModel)
                } else {
                    val preparedCurrencies = LinkedHashMap<String, CurrencyInfo>()
                    for (currencyEntry in state.screenModel.currenciesMap!!) {
                        if (action.changedCurrency.charCode == currencyEntry.key) {
                            preparedCurrencies[currencyEntry.key] = action.changedCurrency
                        } else {
                            val newValue: BigDecimal
                            val newChangedValue = action.changedCurrency.value
                            if (BigDecimal(newChangedValue) == BigDecimal.ZERO) {
                                newValue = BigDecimal.ZERO
                            } else {
                                newValue = BigDecimal(newChangedValue)
                                    .multiply(BigDecimal(currencyEntry.value.value))
                                    .divide(BigDecimal(oldChangedValue), 2, RoundingMode.HALF_EVEN)
                            }

                            preparedCurrencies[currencyEntry.key] =
                                currencyEntry.value.copy(value = newValue.toPlainString())
                        }
                    }
                    State.ShowCurrenciesInfoState(state.screenModel.copy(currenciesMap = preparedCurrencies))
                }*/
            }
            is ShowErrorUpdatingCurrenciesInfoAction -> {
                if (state !is ContainsItems) {
                    throw IllegalStateException("We never loaded currencies")
                }
                State.ShowCurrenciesInfoAndUpdateErrorState(
                    state.screenModel,
                    action.error.localizedMessage ?: action.error.toString()
                )
            }
            is HideErrorUpdatingCurrenciesInfoAction -> {
                if (state !is ContainsItems) {
                    throw IllegalStateException("We never loaded currencies")
                }
                State.ShowCurrenciesInfoState(state.screenModel)
            }
            is Action.LoadCurrenciesInfoAction,
            is SaveCbrfApiResponseAction,
            is PrepareCurrenciesToShowAction,
            is Action.UpdateCurrenciesInfoAction -> state
        }
    }

    private fun saveDataInMemoryCache(date: String, currenciesMap: Map<String, CurrencyInfo>) {
        memoryCash["date"] = date
        memoryCash.putAll(currenciesMap)

        //memoryCash["date"] = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        //memoryCash.putAll(response.currenciesInfo.filter { entry -> getChosenCurrencies().contains(entry.key) })
    }

    // todo get selected currencies from settings
    private fun getChosenCurrencies() = listOf("USD", "EUR", "CNY")

    fun create(coroutineScope: CoroutineScope): Store<State, Action> = coroutineScope
        .createStore(
            name = "Main State Machine",
            initialState = State.LoadingCurrenciesInfoState,
            logSinks = loggers.toList(),
            sideEffects = listOf(
                loadCurrenciesInfoSideEffect,
                saveCbrfResponseSideEffect,
                prepareCurrenciesSideEffect
            ),
            reducer = ::reducer
        )
}