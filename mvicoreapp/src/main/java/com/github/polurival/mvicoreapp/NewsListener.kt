package com.github.polurival.mvicoreapp

import android.content.Context
import android.widget.Toast
import com.github.polurival.mvicoreloadcurrenciesfeature.LoadCurrenciesFeature

import io.reactivex.functions.Consumer

class NewsListener(
    private val context: Context
) : Consumer<LoadCurrenciesFeature.News> {

    override fun accept(news: LoadCurrenciesFeature.News) {
        when (news) {
            is LoadCurrenciesFeature.News.ErrorExecutingRequest -> errorHappened(news.throwable)
        }
    }

    fun errorHappened(throwable: Throwable) {
        Toast.makeText(context, "Simulated error was triggered", Toast.LENGTH_SHORT).show()
    }
}
