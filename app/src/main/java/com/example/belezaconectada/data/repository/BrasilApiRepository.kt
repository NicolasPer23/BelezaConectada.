package com.example.belezaconectada.data.repository

import com.example.belezaconectada.data.model.CepResposta
import com.example.belezaconectada.data.model.CnpjResposta
import com.example.belezaconectada.data.remote.RetrofitClient

class BrasilApiRepository {
    private val apiService = RetrofitClient.brasilApi

    suspend fun consultarCnpj(cnpj: String): CnpjResposta {
        return apiService.consultarCnpj(cnpj)
    }

    suspend fun consultarCep(cep: String): CepResposta {
        return apiService.consultarCep(cep)
    }
}
