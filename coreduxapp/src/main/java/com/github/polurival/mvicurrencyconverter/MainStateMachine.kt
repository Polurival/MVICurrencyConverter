package com.github.polurival.mvicurrencyconverter

import com.freeletics.coredux.*
import com.github.polurival.datalib.common.ConverterScreenModel
import com.github.polurival.datalib.common.CurrencyInfo
import com.github.polurival.datalib.local.LocalStorage
import com.github.polurival.datalib.remote.CbrfApi
import com.github.polurival.datalib.remote.CbrfResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.LinkedHashMap

/**
 * Input Actions from UI
 *
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

// region inner actions

private object StartLoadingCurrenciesInfoAction : Action() {
    override fun toString(): String = StartLoadingCurrenciesInfoAction::class.java.simpleName
}

private data class ErrorLoadingCurrenciesInfoAction(val error: Throwable) : Action() {
    override fun toString(): String =
        "${ErrorLoadingCurrenciesInfoAction::class.java.simpleName} + error=$error"
}

private data class SaveCbrfApiResponseAction(
    val response: CbrfResponse,
    val focusedCurrency: CurrencyInfo?
) : Action() {
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

// endregion inner actions

/**
 * todo Interactor, that will use CbrfApi, CurrenciesInfoStorage and memory cash and contains business logic
 * Move memory cash and work with it to CurrenciesInfoStorage
 */
class MainStateMachine @Inject constructor(
    private val cbrfApi: CbrfApi,
    private val currencyInfoStorage: LocalStorage,
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
                logger.logCustom(name, "Start loading currencies info")
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
                logger.logCustom(name, "Start saving Cbrf response")
                saveCbrfResponse(action, output, name, logger)
            }
        } else {
            null
        }
    }

    private val prepareCurrenciesSideEffect = CancellableSideEffect<State, Action>(
        name = "Prepare currencies to show"
    ) { state, action, logger, handler ->
        val currentState = state()
        if (action is PrepareCurrenciesToShowAction) {
            handler { name, output ->
                logger.logCustom(name, "Start preparing currencies to show")
                prepareCurrenciesToShow(currentState, action, output, name, logger)
            }
        } else {
            null
        }
    }

    /**
     * Loads data from Preferences and Room or from memory cash and calculate currencies values for UI
     */
    private fun CoroutineScope.prepareCurrenciesToShow(
        state: State,
        action: PrepareCurrenciesToShowAction,
        output: SendChannel<Action>,
        sideEffectName: String,
        logger: SideEffectLogger
    ): Job = launch {
        try {
            val screenModel: ConverterScreenModel
            if (action.screenModel?.currenciesMap == null) {
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

                        ConverterScreenModel(loadDate, action.screenModel?.focusedCurrencyInfo, currenciesMap)
                    }.await()
                }
            } else {
                screenModel = action.screenModel
            }

            val preparedCurrencies = LinkedHashMap<String, CurrencyInfo>()
            val date: String?
            val focusedCurrencyInfo = screenModel.focusedCurrencyInfo
            if (state is ContainsItems && focusedCurrencyInfo != null) {
                // обновление
                val focusedCharCode = focusedCurrencyInfo.charCode
                val focusedValue = BigDecimal(focusedCurrencyInfo.value)
                val focusedNominal = BigDecimal(focusedCurrencyInfo.nominal)

                date = memoryCash["date"] as String
                for (key in memoryCash.keys) {
                    if (key == "date") {
                        continue
                    }
                    val currencyInfo = memoryCash[key] as CurrencyInfo
                    if (key == focusedCharCode) {
                        preparedCurrencies[key] = focusedCurrencyInfo
                    } else {
                        val newValue = focusedValue.multiply(BigDecimal(currencyInfo.nominal))
                            .divide(BigDecimal(currencyInfo.value), 2, RoundingMode.HALF_EVEN)
                            .divide(focusedNominal, 2, RoundingMode.HALF_EVEN)
                        preparedCurrencies[key] = currencyInfo.copy(value = newValue.toPlainString())
                    }
                }
            } else {
                // первоначальная загрузка
                date = null
                for (currencyEntry in screenModel.currenciesMap!!) {
                    val newValue = BigDecimal(currencyEntry.value.nominal * 100)
                        .divide(BigDecimal(currencyEntry.value.value), 2, RoundingMode.HALF_EVEN)
                    preparedCurrencies[currencyEntry.key] =
                        currencyEntry.value.copy(value = newValue.toPlainString())
                }
            }

            ShowCurrenciesInfoAction(
                screenModel.copy(
                    date = date ?: screenModel.date,
                    currenciesMap = preparedCurrencies
                )
            ).run {
                logger.logDispatchingToReducer(sideEffectName, this)
                output.send(this)
            }
        } catch (error: Throwable) {
            logger.logCustom(sideEffectName, "Error on preparing currencies to show: $error")
            ErrorLoadingCurrenciesInfoAction(error)
                .run {
                    logger.logDispatchingToReducer(sideEffectName, this)
                    output.send(this)
                }
            // todo добавить предыдущее состояние, которое скрывает состояние ошибки
        }
    }

    /**
     * Saves load date in Preferences and currencies info in Room
     */
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
                    currenciesInfo.add(0, getDefaultRubCurrencyInfo())
                    currencyInfoStorage.saveCurrencies(currenciesInfo)
                }.await()
            }
            PrepareCurrenciesToShowAction(ConverterScreenModel(focusedCurrencyInfo = action.focusedCurrency))
                .run {
                    logger.logDispatchingToReducer(sideEffectName, this)
                    output.send(this)
                }
        } catch (error: Throwable) {
            logger.logCustom(sideEffectName, "Error on saving Cbrf response info: $error")
            ErrorLoadingCurrenciesInfoAction(error)
                .run {
                    logger.logDispatchingToReducer(sideEffectName, this)
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
                logger.logDispatchingToReducer(sideEffectName, this)
                output.send(this)
            }

        delay(TimeUnit.SECONDS.toMillis(1)) // Add some delay to make the loading indicator appear

        try {
            val result = withContext(Dispatchers.IO) {
                async {
                    val todayDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                    val lastLoadDate = currencyInfoStorage.loadDate()

                    // todo добавить условие, что если интернет отсутствует или плохое соединение, возвращать здесь сразу null
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
                getDefaultRubCurrencyInfo()
            }
            if (result == null) {
                // если происходит первоначальная загрузка и в базе данных уже есть актуальные данные
                PrepareCurrenciesToShowAction(ConverterScreenModel(focusedCurrencyInfo = focusedCurrency))
                    .run {
                        logger.logDispatchingToReducer(sideEffectName, this)
                        output.send(this)
                    }
            } else {
                SaveCbrfApiResponseAction(result, focusedCurrency)
                    .run {
                        logger.logDispatchingToReducer(sideEffectName, this)
                        output.send(this)
                    }
            }
        } catch (error: Throwable) {
            logger.logCustom(sideEffectName, "Error on loading currencies info: $error")
            ErrorLoadingCurrenciesInfoAction(error)
                .run {
                    logger.logDispatchingToReducer(sideEffectName, this)
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
                State.ShowCurrenciesInfoState(action.screenModel)
            }
            // это действие вызывается при редактировании значения пользователем
            is Action.CalculateNewValuesAction -> {
                if (state !is ContainsItems) {
                    throw IllegalStateException("We never loaded currencies")
                }
                State.ShowCurrenciesInfoState(state.screenModel.copy(currenciesMap = convertCurrencies(action)))
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

    private fun convertCurrencies(action: Action.CalculateNewValuesAction): LinkedHashMap<String, CurrencyInfo> {
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
        return preparedCurrencies
    }

    private fun saveDataInMemoryCache(date: String, currenciesMap: Map<String, CurrencyInfo>) {
        memoryCash["date"] = date
        memoryCash.putAll(currenciesMap)
    }

    // todo get selected currencies from settings
    private fun getChosenCurrencies() = listOf("RUB", "USD", "EUR", "CNY")

    // todo внедрить менеджер ресурсов
    private fun getDefaultRubCurrencyInfo() = CurrencyInfo("RUB", 1, "Российский рубль", "1")

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

private fun SideEffectLogger.logCustom(sideEffectName: String, error: String) {
    logSideEffectEvent {
        LogEvent.SideEffectEvent.Custom(sideEffectName, error)
    }
}

private fun SideEffectLogger.logDispatchingToReducer(sideEffectName: String, action: Action) {
    logSideEffectEvent {
        LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, action)
    }
}