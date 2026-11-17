package com.example.belezaconectada.data.remote

import com.example.belezaconectada.data.model.CepResposta
import com.example.belezaconectada.data.model.CnpjResposta
import retrofit2.http.GET
import retrofit2.http.Path

interface BrasilApiService {
    @GET("api/cnpj/v1/{cnpj}")
    suspend fun consultarCnpj(@Path("cnpj") cnpj: String): CnpjResposta

    @GET("api/cep/v1/{cep}")
    suspend fun consultarCep(@Path("cep") cep: String): CepResposta
}
