package com.github.polurival.mvicurrencyconverter

import com.freeletics.coredux.*
import com.github.polurival.mvicurrencyconverter.cbrf.CbrfApiFacade
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

/**
 * @author Польщиков Юрий on 2019-07-06
 */
sealed class Action {

    object LoadCurrenciesInfoAction : Action() {
        override fun toString(): String = LoadCurrenciesInfoAction::class.java.simpleName
    }

    object UpdateCurrenciesInfoAction : Action() {
        override fun toString(): String = UpdateCurrenciesInfoAction::class.java.simpleName
    }
}

private object StartLoadingCurrenciesInfoAction : Action() {
    override fun toString(): String = StartLoadingCurrenciesInfoAction::class.java.simpleName
}

private data class ErrorLoadingCurrenciesInfoAction(val error: Throwable) : Action() {
    override fun toString(): String =
        "${ErrorLoadingCurrenciesInfoAction::class.java.simpleName} + error=$error"
}

private data class ShowCurrenciesInfoAction(val currenciesInfo: List<CurrencyInfo>) : Action() {
    override fun toString(): String =
        "${ShowCurrenciesInfoAction::class.java.simpleName} + currenciesCount=${currenciesInfo.size}"
}

private data class ShowErrorUpdatingCurrenciesInfoAction(val error: Throwable) : Action() {
    override fun toString(): String =
        "${ShowErrorUpdatingCurrenciesInfoAction::class.java.simpleName} + error=$error"
}

private data class HideErrorUpdatingCurrenciesInfoAction(val error: Throwable) : Action() {
    override fun toString(): String =
        "${HideErrorUpdatingCurrenciesInfoAction::class.java.simpleName} + error=$error"
}

class MainStateMachine(private val cbrfApiFacade: CbrfApiFacade) {

    private interface ContainsItems {
        val currenciesInfo: List<CurrencyInfo>
    }

    sealed class State {

        object LoadingCurrenciesInfoState : State() {
            override fun toString(): String = LoadingCurrenciesInfoState::class.java.simpleName
        }

        data class ErrorLoadingCurrenciesInfoState(val errorMessage: String) : State() {
            override fun toString(): String =
                "${ErrorLoadingCurrenciesInfoState::class.java.simpleName} error=$errorMessage"
        }

        data class ShowCurrenciesInfoState(override val currenciesInfo: List<CurrencyInfo>) : State(), ContainsItems {
            override fun toString(): String =
                "${ShowCurrenciesInfoState::class.java.simpleName} currenciesCount=${currenciesInfo.size}"
        }

        data class ShowCurrenciesAndUpdateState(override val currenciesInfo: List<CurrencyInfo>) : State(),
            ContainsItems {
            override fun toString(): String =
                "${ShowCurrenciesAndUpdateState::class.java.simpleName} currenciesCount=${currenciesInfo.size}"
        }

        data class ShowCurrenciesInfoAndUpdateErrorState(
            override val currenciesInfo: List<CurrencyInfo>,
            val errorMessage: String
        ) : State(), ContainsItems {
            override fun toString(): String =
                "${ShowCurrenciesInfoAndUpdateErrorState::class.java.simpleName} " +
                        "currenciesCount=${currenciesInfo.size} error=$errorMessage"
        }
    }

    /**
     * Load the first Page
     */
    /*private val loadFirstPageSideEffect = CancellableSideEffect<State, Action>(
        name = "Load First Page"
    ) { state, action, logger, handler ->
        val currentState = state()
        if (action is Action.LoadFirstPageAction &&
            currentState !is ContainsItems) {
            handler { name, output ->
                logger.logSideEffectEvent { LogEvent.SideEffectEvent.Custom(name, "Start loading first page") }
                nextPage(currentState, output, name, logger)
            }
        } else {
            null
        }
    }*/

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
                loadCurrenciesInfo(currentState, output, name, logger)
            }
        } else {
            null
        }
    }

    fun create(coroutineScope: CoroutineScope): Store<State, Action> = coroutineScope
        .createStore(
            name = "Main State Machine",
            initialState = State.LoadingCurrenciesInfoState,
            sideEffects = listOf(loadCurrenciesInfoSideEffect),
            reducer = ::reducer
        )

    /**
     * Loads currencies info from rest service
     */
    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.loadCurrenciesInfo(
        state: State,
        output: SendChannel<Action>,
        sideEffectName: String,
        logger: SideEffectLogger
    ): Job = launch {

        StartLoadingCurrenciesInfoAction.run {
            logger.logSideEffectEvent {
                LogEvent.SideEffectEvent.DispatchingToReducer(sideEffectName, this)
            }
            output.send(this)
        }

        try {
            // todo
        } catch (error: Throwable) {
            logger.logSideEffectEvent {
                LogEvent.SideEffectEvent.Custom(sideEffectName, "Error on loading currencies info: $error")
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
                    State.ShowCurrenciesAndUpdateState(state.currenciesInfo)
                } else {
                    State.LoadingCurrenciesInfoState
                }
            }
            is ErrorLoadingCurrenciesInfoAction -> {
                State.ErrorLoadingCurrenciesInfoState(action.error.localizedMessage ?: action.error.toString())
            }
            is ShowCurrenciesInfoAction -> {
                State.ShowCurrenciesInfoState(action.currenciesInfo)
            }
            is ShowErrorUpdatingCurrenciesInfoAction -> {
                if (state !is ContainsItems) {
                    throw IllegalStateException("We never loaded currencies")
                }
                State.ShowCurrenciesInfoAndUpdateErrorState(
                    state.currenciesInfo,
                    action.error.localizedMessage ?: action.error.toString()
                )
            }
            is HideErrorUpdatingCurrenciesInfoAction -> {
                if (state !is ContainsItems) {
                    throw IllegalStateException("We never loaded currencies")
                }
                State.ShowCurrenciesInfoState(state.currenciesInfo)
            }
            is Action.LoadCurrenciesInfoAction,
            is Action.UpdateCurrenciesInfoAction -> state
        }
    }
}