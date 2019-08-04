package com.github.polurival.mvicurrencyconverter

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.polurival.datalib.common.CurrencyInfo
import com.github.polurival.datalib.util.viewModel
import kotlinx.android.synthetic.main.mini_converter_activity.*
import javax.inject.Inject
import javax.inject.Provider

/**
 * todo переделать DI, сделать без @Inject
 *
 * @author Польщиков Юрий on 2019-06-06
 */
class MainActivity : AppCompatActivity(),
    SwipeRefreshLayout.OnRefreshListener, MainAdapter.ValueChangeListener {

    @Inject
    lateinit var viewModelProvider: Provider<MainViewModel>

    private val viewModel by lazy {
        viewModel<MainViewModel>(ViewModelProviderFactory(viewModelProvider))
    }

    @Inject
    lateinit var viewBindingFactory: ViewBindingFactory

    private val viewBinding by lazy {
        viewBindingFactory.create<MainViewBinding>(
            MainActivity::class.java,
            rootView
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mini_converter_activity)
        applicationComponent.inject(this)
        viewBinding.setRefreshListener(this)
        viewBinding.setValueChangeListener(this)

        viewModel.state.observe(this, Observer {
            viewBinding.render(it!!)
        })

        viewModel.dispatchAction(Action.LoadCurrenciesInfoAction)
    }

    override fun onRefresh() {
        viewModel.dispatchAction(Action.UpdateCurrenciesInfoAction(viewBinding.getScreenModel()))
    }

    override fun onValueChanged(currencyInfo: CurrencyInfo) {
        viewModel.dispatchAction(Action.CalculateNewValuesAction(currencyInfo))
    }

    private val Activity.applicationComponent
        get() = (application as App).applicationComponent
}
