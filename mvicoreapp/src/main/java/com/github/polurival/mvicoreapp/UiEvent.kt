package com.github.polurival.mvicoreapp

/**
 * @author Польщиков Юрий on 2019-07-24
 */
sealed class UiEvent {
    object ScreenOpened : UiEvent()
    data class ValueChanged(val value: String) : UiEvent()
}