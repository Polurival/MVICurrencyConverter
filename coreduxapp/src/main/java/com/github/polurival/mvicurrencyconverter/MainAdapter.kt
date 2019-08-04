package com.github.polurival.mvicurrencyconverter

import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.polurival.datalib.common.CurrencyInfo
import com.github.polurival.datalib.util.afterTextChanged

private const val FOCUSED_CURRENCY_VIEW = 1
private const val CURRENCY_VIEW = 2
private const val EMPTY_VIEW = 3
private val ANY_NON_DIGIT_REGEXP = Regex("^\\D+\$")

/**
 * @author Польщиков Юрий on 2019-07-25
 */
class MainAdapter(
    private val layoutInflater: LayoutInflater,
    private var valueChangeListener: ValueChangeListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = LinkedHashMap<String, CurrencyInfo>()
    private var focusedPosition = 0

    override fun getItemViewType(position: Int): Int =
        if (position == focusedPosition) {
            FOCUSED_CURRENCY_VIEW
        } else {
            CURRENCY_VIEW
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            FOCUSED_CURRENCY_VIEW -> {
                CurrencyViewHolder(
                    layoutInflater.inflate(R.layout.currency_item_focused, parent, false),
                    valueChangeListener
                )
            }
            CURRENCY_VIEW -> {
                CurrencyViewHolder(
                    layoutInflater.inflate(R.layout.currency_item, parent, false),
                    null
                )
            }
            EMPTY_VIEW -> {
                EmptyViewHolder(layoutInflater.inflate(R.layout.empty_item, parent, false))
            }
            else -> throw IllegalArgumentException("ViewType $viewType is unexpected")
        }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CurrencyViewHolder) {
            holder.bind(items.values.toList()[position])
        }

        if (position != focusedPosition) {
            holder.itemView.isClickable = false;
            val itemViewGroup = holder.itemView as ViewGroup
            for (i in 0 until itemViewGroup.childCount) {
                itemViewGroup.getChildAt(i).isClickable = false
                itemViewGroup.getChildAt(i).isFocusable = false
            }
        }

        //todo продумать бизнес-логику - чтобы конвертация была корректна при выборе любой валюты
        /*holder.itemView.setOnClickListener {
            val lastFocusedPosition = focusedPosition
            focusedPosition = position

            notifyItemChanged(lastFocusedPosition)
            notifyItemChanged(position)
        }*/
    }

    fun setValueChangeListener(valueChangeListener: ValueChangeListener) {
        this.valueChangeListener = valueChangeListener
    }

    fun getFocusedCurrency(): CurrencyInfo {
        return items.values.toList()[focusedPosition]
    }

    inner class CurrencyViewHolder(
        view: View,
        private val valueChangeListener: ValueChangeListener?
    ) : RecyclerView.ViewHolder(view) {

        private val charCodeView: TextView = itemView.findViewById(R.id.charCodeView)
        private val editTextView: EditText = itemView.findViewById(R.id.valueView)
        private val nameView: TextView = itemView.findViewById(R.id.nameView)

        init {
            editTextView.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            editTextView.filters = arrayOf(InputFilter.LengthFilter(20))
        }

        fun bind(item: CurrencyInfo) {

            charCodeView.text = item.charCode
            nameView.text = item.name
            editTextView.setText(item.value)


            editTextView.afterTextChanged { value ->
                val text: String
                if (value.isBlank()) {
                    text = editTextView.hint.toString()
                } else if (value.contains(ANY_NON_DIGIT_REGEXP)) {
                    text = "0"
                } else {
                    text = value
                }
                valueChangeListener?.onValueChanged(item.copy(value = text))
            }
        }
    }

    inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface ValueChangeListener {
        fun onValueChanged(currencyInfo: CurrencyInfo)
    }
}