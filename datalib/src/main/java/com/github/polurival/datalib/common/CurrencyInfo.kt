package com.github.polurival.datalib.common

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

/**
 * Parcelable здесь только для MviCore - TimeCapsule
 *
 * @author Польщиков Юрий on 2019-07-06
 */
@Parcelize
@Entity(tableName = "currencyInfo")
data class CurrencyInfo(
    /**
     * ISO 4217 буквенный код валюты
     */
    @PrimaryKey
    @ColumnInfo(name = "charCode")
    val charCode: String,
    /**
     * номинал валюты
     */
    @ColumnInfo(name = "nominal")
    val nominal: Int,
    /**
     * имя валюты
     */
    @ColumnInfo(name = "name")
    val name: String,
    /**
     * количество по отношению к 1 RUB
     */
    @ColumnInfo(name = "value")
    val value: String
) : Parcelable
