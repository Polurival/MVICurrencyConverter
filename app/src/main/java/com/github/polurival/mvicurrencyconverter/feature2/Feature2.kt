package com.github.polurival.mvicurrencyconverter.feature2

import android.os.Parcelable
import android.util.Log
import com.badoo.mvicore.element.*
import com.badoo.mvicore.feature.BaseFeature
import com.github.polurival.mvicurrencyconverter.cbrf.ManualCbrfApiFacade
import com.github.polurival.mvicurrencyconverter.data.CurrenciesInfoStorage
import com.github.polurival.mvicurrencyconverter.dto.CbrfResponse
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Action
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Action.Execute
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Action.SaveToDatabase
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Effect
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Effect.ErrorLoading
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Effect.ErrorSaving
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Effect.LoadedCurrencies
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Effect.SavedCurrencies
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Effect.StartedLoading
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.News
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.State
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Wish
import com.github.polurival.mvicurrencyconverter.feature2.Feature2.Wish.LoadCurrenciesInfo
import io.reactivex.Observable
import io.reactivex.Observable.just
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.parcel.Parcelize

/**
 * Feature (или Store, или StateMachine) загрузки и отображения валют
 *
 * @author Польщиков Юрий on 2019-07-22
 */
class Feature2(
    storage: CurrenciesInfoStorage,
    timeCapsule: TimeCapsule<Parcelable>? = null
) : BaseFeature<Wish, Action, Effect, State, News>(
    initialState = timeCapsule?.get(Feature2::class.java) ?: State(),
    bootstrapper = BootStrapperImpl(),
    wishToAction = { Execute(it) },
    actor = ActorImpl(storage),
    reducer = ReducerImpl(),
    postProcessor = PostProcessorImpl(),
    newsPublisher = NewsPublisherImpl()
) {
    init {
        timeCapsule?.register(Feature2::class.java) {
            state.copy(
                isLoading = false
            )
        }
    }

    @Parcelize
    data class State(
        val isLoading: Boolean = false,
        val date: String? = null,
        val currenciesInfo: List<CurrencyInfo>? = null
    ) : Parcelable

    sealed class Wish {
        object LoadCurrenciesInfo : Wish()
    }

    sealed class Action {
        data class Execute(val wish: Wish) : Action()
        data class SaveToDatabase(val response: CbrfResponse) : Action()
    }

    sealed class Effect {
        object StartedLoading : Effect()
        data class LoadedCurrencies(val response: CbrfResponse) : Effect()
        data class ErrorLoading(val throwable: Throwable) : Effect()

        data class SavedCurrencies(val currenciesInfo: List<CurrencyInfo>) : Effect()
        data class ErrorSaving(val throwable: Throwable) : Effect()
    }

    sealed class News {
        data class ErrorExecutingRequest(val throwable: Throwable) : News()
    }

    class BootStrapperImpl : Bootstrapper<Action> {
        override fun invoke(): Observable<Action> = just(Execute(LoadCurrenciesInfo))
    }

    class ActorImpl(private val storage: CurrenciesInfoStorage) : Actor<State, Action, Effect> {

        override fun invoke(state: State, action: Action): Observable<Effect> = when (action) {
            is Execute -> when (action.wish) {
                is LoadCurrenciesInfo -> requestCurrencies()
                    .map { LoadedCurrencies(it) as Effect }
                    .startWith(just(StartedLoading))
                    .onErrorReturn { ErrorLoading(it) }
            }
            is SaveToDatabase -> saveCurrenciesToDatabase(action.response)
                .map { SavedCurrencies(it) as Effect }
                .onErrorReturn { ErrorSaving(it) }
        }

        private fun requestCurrencies(): Observable<CbrfResponse> {
            return Observable.create<CbrfResponse> { emitter ->
                try {
                    Thread.sleep(2000)
                    emitter.onNext(ManualCbrfApiFacade.service.loadCurrenciesInfo())
                    emitter.onComplete()
                } catch (e: Throwable) {
                    emitter.tryOnError(e)
                }
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }

        private fun saveCurrenciesToDatabase(response: CbrfResponse): Observable<List<CurrencyInfo>> {
            return Observable.create<List<CurrencyInfo>> { emitter ->
                try {
                    storage.saveDate(response.date)
                    val currenciesInfo = response.currenciesInfo.values.toList()
                    storage.saveCurrencies(currenciesInfo)
                    emitter.onNext(currenciesInfo)
                    Log.d("abra", "currencies was saved")
                    emitter.onComplete()
                } catch (e: Throwable) {
                    emitter.tryOnError(e)
                }
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }
    }

    class PostProcessorImpl : PostProcessor<Action, Effect, State> {
        // do anything based on action (contains wish), effect, state
        override fun invoke(action: Action, effect: Effect, state: State): Action? {
            when (effect) {
                is LoadedCurrencies -> SaveToDatabase(effect.response)
            }
            return null
        }
    }

    class ReducerImpl : Reducer<State, Effect> {
        override fun invoke(state: State, effect: Effect): State = when (effect) {
            StartedLoading -> state.copy(
                isLoading = true
            )
            is LoadedCurrencies -> state.copy(
                date = effect.response.date
            )
            is ErrorLoading -> state.copy(
                isLoading = false
            )

            is SavedCurrencies -> state.copy(
                isLoading = false,
                currenciesInfo = effect.currenciesInfo
            )
            is ErrorSaving -> state.copy(
                isLoading = false
            )
        }
    }

    class NewsPublisherImpl : NewsPublisher<Action, Effect, State, News> {
        override fun invoke(action: Action, effect: Effect, state: State): News? = when (effect) {
            is ErrorLoading -> News.ErrorExecutingRequest(effect.throwable)
            is ErrorSaving -> News.ErrorExecutingRequest(effect.throwable)
            else -> null
        }
    }

}