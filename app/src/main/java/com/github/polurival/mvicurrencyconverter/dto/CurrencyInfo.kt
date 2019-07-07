package com.github.polurival.mvicurrencyconverter.dto

/**
 * @author Польщиков Юрий on 2019-07-06
 */
data class CurrencyInfo(
    /**
     * код валюты
     */
    val charCode: String,
    /**
     * номинал
     */
    val nominal: Int,
    /**
     * количество по отношению к 1 рублю ???
     */
    val rate: Double
)