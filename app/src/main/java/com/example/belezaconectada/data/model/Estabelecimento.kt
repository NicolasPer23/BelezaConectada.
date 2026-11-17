package com.example.belezaconectada.data.model

data class Estabelecimento(
    val id: String,
    val nome: String,
    val subcategoria: String,
    val categoria: String,
    val descricao: String,
    val tag: String,
    val avaliacao: String,
    val avaliacoesCount: String,
    val distancia: String,
    val status: String,
    val preco: String,
    var favorito: Boolean,
    val imagemUrl: String = ""
)
