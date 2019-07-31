package com.github.polurival.mvicurrencyconverter

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import com.github.polurival.mvicurrencyconverter.util.viewModel
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.mini_converter_activity.*
import javax.inject.Inject
import javax.inject.Provider

/**
 * @author Польщиков Юрий on 2019-06-06
 */
class MainActivity : AppCompatActivity(), MainAdapter.ValueChangeListener {

    override fun onCreateObservable(observable: Observable<CurrencyInfo>) {
        disposables.add(observable.map {
            Action.CalculateNewValuesAction(it)
        }.subscribe(viewModel.dispatchAction))
    }

    @Inject
    lateinit var viewModelProvider: Provider<MainViewModel>

    private val viewModel by lazy {
        viewModel<MainViewModel>(SimpleViewModelProviderFactory(viewModelProvider))
    }

    // todo пришлось отказаться для передачи слушателя в конструкторе. Попробовать сделать через сеттер и вернуть ДИ здесь
    //@Inject
    //lateinit var viewBindingFactory: ViewBindingFactory

    /*private val viewBinding by lazy {
        viewBindingFactory.create<MainViewBinding>(
            MainActivity::class.java,
            rootView
        )
    }*/
    private lateinit var viewBinding: MainViewBinding

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mini_converter_activity)
        viewBinding = MainViewBinding(rootView, this)
        applicationComponent.inject(this)

        viewModel.state.observe(this, Observer {
            viewBinding.render(it!!)
        })

        disposables.add(
            viewBinding.refreshSwiped.map {
                Action.UpdateCurrenciesInfoAction(viewBinding.getScreenModel())
            }.subscribe(viewModel.dispatchAction)
        )

        viewModel.dispatchAction(Action.LoadCurrenciesInfoAction)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    private val Activity.applicationComponent
        get() = (application as App).applicationComponent
}
