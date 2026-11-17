package com.example.belezaconectada.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val brasilApi: BrasilApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://brasilapi.com.br/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BrasilApiService::class.java)
    }
}
