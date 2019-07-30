package com.github.polurival.mvicurrencyconverter

import android.annotation.SuppressLint
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo
import io.reactivex.Observable

const val FOCUSED_CURRENCY_VIEW = 1
const val CURRENCY_VIEW = 2

/**
 * @author Польщиков Юрий on 2019-07-25
 */
class MainAdapter(
    private val layoutInflater: LayoutInflater,
    private val valueChangeListener: ValueChangeListener
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
            else -> throw IllegalArgumentException("ViewType $viewType is unexpected")
        }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CurrencyViewHolder) {
            holder.bind(items.values.toList()[position])
        }
        if (!holder.itemView.hasOnClickListeners()) {
            holder.itemView.setOnClickListener {
                focusedPosition = position
                // todo перерисовывать только прошлый и текущий фокусные элементы
                notifyDataSetChanged()
            }
        }
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

        @SuppressLint("CheckResult")
        fun bind(item: CurrencyInfo) {

            charCodeView.text = item.charCode
            nameView.text = item.name
            editTextView.setText(item.value)
            editTextView.hint = "100"

            valueChangeListener?.onCreateObservable(Observable.create<CurrencyInfo> { emitter ->
                val textWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        // do nothing
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        // do nothing
                    }

                    override fun afterTextChanged(editable: Editable?) {
                        var text = editable.toString()
                        text = if (text.isBlank()) editTextView.hint.toString() else text
                        emitter.onNext(item.copy(value = text))
                    }
                }
                emitter.setCancellable {
                    editTextView.removeTextChangedListener(textWatcher)
                }

                editTextView.addTextChangedListener(textWatcher)
            })
        }
    }

    interface ValueChangeListener {
        fun onCreateObservable(observable: Observable<CurrencyInfo>)
    }
}