package com.github.polurival.mvicoreapp

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.ListAdapter
import androidx.room.Room
import com.github.polurival.datalib.common.CurrencyInfo
import com.github.polurival.datalib.local.CurrenciesInfoStorage
import com.github.polurival.datalib.local.CurrencyInfoDatabase
import com.github.polurival.mvicoreloadcurrenciesfeature.LoadCurrenciesFeature
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.mini_converter_activity.*

class MiniConverterActivity : ObservableSourceActivity<UiEvent>(), Consumer<ViewModel> {

    lateinit var bindings: MiniConverterActivityBindings

    /**
     * todo DI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currencyInfoDatabase = Room.databaseBuilder(
            this.applicationContext,
            CurrencyInfoDatabase::class.java,
            "currencyInfoDatabase"
        ).build()
        bindings = MiniConverterActivityBindings(
            this, LoadCurrenciesFeature(CurrenciesInfoStorage(this, currencyInfoDatabase)),
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

    @Suppress("UNCHECKED_CAST")
    override fun accept(vm: ViewModel) {
        toolbar.title = vm.date
        progress.visibility = if (vm.currenciesIdLoading) View.VISIBLE else View.GONE

        (currenciesList.adapter as ListAdapter<CurrencyInfo, CurrenciesViewHolder>).submitList(vm.currenciesInfo)
    }
}
