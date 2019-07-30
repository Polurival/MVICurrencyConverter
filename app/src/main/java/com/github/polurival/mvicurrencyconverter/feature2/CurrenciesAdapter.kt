package com.github.polurival.mvicurrencyconverter.feature2

import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.polurival.mvicurrencyconverter.R
import com.github.polurival.mvicurrencyconverter.common.ObservableSourceActivity
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import com.github.polurival.mvicurrencyconverter.util.afterTextChanged
import kotlinx.android.synthetic.main.currency_item.view.*

/**
 * @author Польщиков Юрий on 2019-07-24
 */
class CurrenciesAdapter(private val observableSource: ObservableSourceActivity<UiEvent>) :
    ListAdapter<CurrencyInfo, CurrenciesViewHolder>(CountryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrenciesViewHolder {
        return CurrenciesViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.currency_item, parent, false),
            observableSource
        )
    }

    override fun onBindViewHolder(holder: CurrenciesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CurrenciesViewHolder(
    itemView: View,
    private val observableSource: ObservableSourceActivity<UiEvent>,
    private val charCode: TextView = itemView.findViewById(R.id.charCodeView),
    private val editText: EditText = itemView.findViewById(R.id.valueView),
    private val name: TextView = itemView.findViewById(R.id.nameView)
) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: CurrencyInfo) {
        charCode.text = item.charCode
        name.text = item.name
        editText.setText(item.value)
        editText.afterTextChanged { value -> observableSource.onNext(UiEvent.ValueChanged(value)) }
    }
}

class CountryDiffCallback : DiffUtil.ItemCallback<CurrencyInfo>() {
    override fun areItemsTheSame(oldItem: CurrencyInfo, newItem: CurrencyInfo): Boolean =
        oldItem.charCode == newItem.charCode

    override fun areContentsTheSame(oldItem: CurrencyInfo, newItem: CurrencyInfo): Boolean = oldItem == newItem
}
