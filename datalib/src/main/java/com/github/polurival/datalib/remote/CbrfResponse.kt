package com.github.polurival.datalib.remote

import android.os.Parcelable
import com.github.polurival.datalib.common.CurrencyInfo
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