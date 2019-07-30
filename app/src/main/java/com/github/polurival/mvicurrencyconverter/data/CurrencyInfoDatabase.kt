package com.github.polurival.mvicurrencyconverter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.polurival.mvicurrencyconverter.dto.CurrencyInfo

/**
 * @author Польщиков Юрий on 2019-07-24
 */
@Database(entities = [CurrencyInfo::class], version = 1)
abstract class CurrencyInfoDatabase : RoomDatabase() {

    abstract fun currencyInfoDao(): CurrencyInfoDao

    companion object {
        @Volatile
        private var INSTANCE: CurrencyInfoDatabase? = null

        fun getDatabase(context: Context): CurrencyInfoDatabase {
            return INSTANCE ?: synchronized(this) {
                // Create database here
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CurrencyInfoDatabase::class.java,
                    "currencyInfoDatabase"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}