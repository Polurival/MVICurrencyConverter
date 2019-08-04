package com.github.polurival.datalib.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.polurival.datalib.common.CurrencyInfo
import com.github.polurival.datalib.local.CurrencyInfoDao

/**
 * todo Schema export directory is not provided to the annotation processor so we cannot export the schema.
 * You can either provide `room.schemaLocation` annotation processor argument OR set exportSchema to false.
 *
 * @author Польщиков Юрий on 2019-07-24
 */
@Database(entities = [CurrencyInfo::class], version = 1)
abstract class CurrencyInfoDatabase : RoomDatabase() {

    abstract fun currencyInfoDao(): CurrencyInfoDao
}