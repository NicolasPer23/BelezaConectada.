package com.example.belezaconectada.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.belezaconectada.data.model.Estabelecimento

class ExplorarViewModel : ViewModel() {
    var textoBusca by mutableStateOf("")
        private set

    var filtroPreco by mutableStateOf("")
        private set

    var filtroDistancia by mutableStateOf("")
        private set

    var categoriaSelecionada by mutableStateOf("")
        private set

    val listaEstabelecimentos = listOf(
        Estabelecimento("1", "Studio Aura", "Salão de beleza", "Cabelo", "Cortes e coloração", "Populares", "4.9", "128", "1.2 km", "Aberto", "$$$", false, "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500"),
        Estabelecimento("2", "Esmalteria Bella", "Manicure e Pedicure", "Unhas", "Unhas em gel e fibra", "Promoção", "4.8", "95", "0.8 km", "Aberto", "$$", false, "https://images.unsplash.com/photo-1604654894610-df63bc536371?w=500"),
        Estabelecimento("3", "Barbearia Viking", "Barbearia Tradicional", "Barba", "Corte masculino e barba", "Destaque", "4.7", "210", "2.5 km", "Aberto", "$$", false, "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500"),
        Estabelecimento("4", "Clínica Glow", "Estética Facial", "Estética", "Limpeza de pele e botox", "Novo", "5.0", "42", "3.1 km", "Aberto", "$$$$", false, "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=500")
    )

    fun updateTextoBusca(novoTexto: String) {
        textoBusca = novoTexto
    }

    fun updateFiltroPreco(novoPreco: String) {
        filtroPreco = novoPreco
    }

    fun updateFiltroDistancia(novaDistancia: String) {
        filtroDistancia = novaDistancia
    }

    fun updateCategoriaSelecionada(novaCategoria: String) {
        categoriaSelecionada = novaCategoria
    }

    fun limparFiltros() {
        filtroPreco = ""
        filtroDistancia = ""
        categoriaSelecionada = ""
    }

    fun obterEstabelecimentosFiltrados(): List<Estabelecimento> {
        return listaEstabelecimentos.filter { est ->
            val bateCategoria = categoriaSelecionada.isEmpty() || est.categoria.equals(categoriaSelecionada, ignoreCase = true)
            val bateTexto = textoBusca.isEmpty() || est.nome.contains(textoBusca, ignoreCase = true) || est.subcategoria.contains(textoBusca, ignoreCase = true)
            val batePreco = filtroPreco.isEmpty() || est.preco == filtroPreco
            val distNum = est.distancia.replace(" km", "").toDoubleOrNull() ?: 0.0
            val distMax = filtroDistancia.toDoubleOrNull() ?: 999.0
            val bateDistancia = filtroDistancia.isEmpty() || distNum <= distMax

            bateCategoria && bateTexto && batePreco && bateDistancia
        }
    }
}
