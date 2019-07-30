package com.github.polurival.mvicurrencyconverter.feature2

import android.content.Context
import android.widget.Toast

import io.reactivex.functions.Consumer

class NewsListener(
    private val context: Context
) : Consumer<Feature2.News> {

    override fun accept(news: Feature2.News) {
        when (news) {
            is Feature2.News.ErrorExecutingRequest -> errorHappened(news.throwable)
        }
    }

    fun errorHappened(throwable: Throwable) {
        Toast.makeText(context, "Simulated error was triggered", Toast.LENGTH_SHORT).show()
    }
}
