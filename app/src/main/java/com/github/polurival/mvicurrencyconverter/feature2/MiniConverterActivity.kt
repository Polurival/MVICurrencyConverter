package com.github.polurival.mvicurrencyconverter.feature2

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.ListAdapter
import com.github.polurival.mvicurrencyconverter.R
import com.github.polurival.mvicurrencyconverter.common.ObservableSourceActivity
import com.github.polurival.mvicurrencyconverter.data.CurrenciesInfoStorage
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.mini_converter_activity.*

class MiniConverterActivity : ObservableSourceActivity<UiEvent>(), Consumer<ViewModel> {

    lateinit var bindings: MiniConverterActivityBindings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = MiniConverterActivityBindings(
            this, Feature2(CurrenciesInfoStorage.getStorage(this)),
            NewsListener(this)
        )
        setContentView(R.layout.mini_converter_activity)
        setupViews()
        bindings.setup(this)

        onNext(UiEvent.ScreenOpened)
    }

    private fun setupViews() {
        setSupportActionBar(toolbar)

        currenciesList.adapter = CurrenciesAdapter(this)
    }

    override fun accept(vm: ViewModel) {
        toolbar.title = vm.date
        val currency = vm.currenciesInfo?.get(0)
        progress.visibility = if (vm.currenciesIdLoading) View.VISIBLE else View.GONE

        (currenciesList.adapter as ListAdapter<CurrencyInfo, CurrenciesViewHolder>).submitList(vm.currenciesInfo)
    }
}
