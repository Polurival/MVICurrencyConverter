package com.github.polurival.mvicurrencyconverter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.polurival.mvicurrencyconverter.dto.ConverterScreenModel
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * @author Польщиков Юрий on 2019-07-06
 */
class MainViewBinding(
    private val rootView: ViewGroup,
    valueChangeListener: MainAdapter.ValueChangeListener
) {

    private val toolbar: Toolbar = rootView.findViewById(R.id.toolbar)
    private val refreshView: SwipeRefreshLayout = rootView.findViewById(R.id.refreshView)
    private val recyclerView: RecyclerView = rootView.findViewById(R.id.currenciesList)
    private val adapter: MainAdapter = MainAdapter(LayoutInflater.from(rootView.context), valueChangeListener)
    private val loading: View = rootView.findViewById(R.id.progress)

    init {
        recyclerView.adapter = adapter
    }

    val refreshSwiped: Observable<Unit> = Observable.create<Unit> { emitter ->
        val listener = SwipeRefreshLayout.OnRefreshListener { emitter.onNext(Unit) }
        emitter.setCancellable { refreshView.setOnRefreshListener(null) }

        refreshView.setOnRefreshListener(listener)
    }.debounce(500, TimeUnit.MILLISECONDS)

    fun getScreenModel(): ConverterScreenModel {
        return ConverterScreenModel(focusedCurrencyInfo = adapter.getFocusedCurrency(), currenciesMap = adapter.items)
    }

    fun render(state: MainStateMachine.State) {
        when (state) {
            is MainStateMachine.State.LoadingCurrenciesInfoState -> {
                refreshView.gone()
                loading.visible()
            }
            is MainStateMachine.State.ShowCurrenciesInfoState -> {
                toolbar.title = state.screenModel.date
                if (refreshView.isRefreshing) {
                    refreshView.isRefreshing = false
                }
                showRecyclerView(state.screenModel.currenciesMap!!)
            }
            is MainStateMachine.State.ErrorLoadingCurrenciesInfoState -> {
                refreshView.visible()
                loading.gone()
                if (refreshView.isRefreshing) {
                    refreshView.isRefreshing = false
                }
                showErrorToast(state.errorMessage)
            }
            is MainStateMachine.State.ShowCurrenciesAndUpdateState -> {
                // do nothing
            }
            is MainStateMachine.State.ShowCurrenciesInfoAndUpdateErrorState -> {
                refreshView.visible()
                if (refreshView.isRefreshing) {
                    refreshView.isRefreshing = false
                }
                showErrorToast(state.errorMessage)
            }
        }
    }

    private fun showRecyclerView(items: LinkedHashMap<String, CurrencyInfo>) {
        refreshView.visible()
        loading.gone()

        if (adapter.items.isNotEmpty()) {

            for (charCode in items.keys) {
                adapter.items[charCode] = items[charCode]!!
                if (adapter.getFocusedCurrency().charCode != items[charCode]?.charCode) {
                    adapter.notifyItemChanged(items.keys.indexOf(charCode))
                }
            }
        } else {
            adapter.items = items
            adapter.notifyDataSetChanged()
        }
    }

    private fun showErrorToast(text: CharSequence) {
        Toast.makeText(
            rootView.context, text, Toast.LENGTH_SHORT
        ).show()
    }

    private fun View.gone() {
        visibility = View.GONE
    }

    private fun View.visible() {
        visibility = View.VISIBLE
    }
}