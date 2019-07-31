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

    @Query(
        """SELECT * FROM currencyInfo WHERE charCode IN (:charCode1, :charCode2, :charCode3, :charCode4)
        ORDER BY charCode=(:charCode1) DESC, charCode=(:charCode2) DESC, charCode=(:charCode3) DESC, charCode=(:charCode4) DESC"""
    )
    fun getSelectedCurrencies(
        charCode1: String,
        charCode2: String,
        charCode3: String,
        charCode4: String
    ): List<CurrencyInfo>
}
