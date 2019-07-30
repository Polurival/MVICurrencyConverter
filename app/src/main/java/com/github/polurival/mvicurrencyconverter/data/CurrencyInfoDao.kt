package com.github.polurival.mvicurrencyconverter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo

/**
 * @author Польщиков Юрий on 2019-07-24
 */
@Dao
interface CurrencyInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(currenciesInfo: List<CurrencyInfo>)

    @Query("SELECT * from currencyInfo WHERE charCode IN (:selectedCurrencies)")
    fun getSelectedCurrencies(selectedCurrencies: List<String>) : List<CurrencyInfo>
}
