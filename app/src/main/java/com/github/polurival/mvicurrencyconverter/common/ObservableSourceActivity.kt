package com.github.polurival.mvicurrencyconverter.common

import androidx.appcompat.app.AppCompatActivity
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.subjects.PublishSubject

/**
 * @author Польщиков Юрий on 2019-07-24
 */
abstract class ObservableSourceActivity<T> : AppCompatActivity(), ObservableSource<T> {

    private val source = PublishSubject.create<T>()

    fun onNext(t: T) {
        source.onNext(t)
    }

    override fun subscribe(observer: Observer<in T>) {
        source.subscribe(observer)
    }
}