package com.github.polurival.mvicurrencyconverter.dto

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Parcelable здесь только для MviCore - TimeCapsule
 *
 * @author Польщиков Юрий on 2019-07-22
 */
@Parcelize
data class CbrfResponse(
    val date: String,
    val currenciesInfo: Map<String, CurrencyInfo>
) : Parcelable