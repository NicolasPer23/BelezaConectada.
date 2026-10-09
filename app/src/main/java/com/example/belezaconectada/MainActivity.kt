package com.example.belezaconectada

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.belezaconectada.ui.theme.BelezaConectadaTheme
import  androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

?

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BelezaConectadaTheme {
                TelaInicial()
            }
        }
    }
}
data class Estabelecimento(
    val nome: String,
    val categoria: String,
    val descricao: String
)

@Composable
fun TelaInicial() {
    var pesquisa by remember { mutableStateOf("") }
    var categoriaSelecionada by remember {
        mutableStateOf("Todos")
    }

    val estabelecimentos = listOf(
        Estabelecimento(
            "Studio Bella",
            "Cabelo",
            "Cortes, escovas e coloração"
        ),
        Estabelecimento(
            "Espaço das Unhas",
            "Unhas",
            "Manicure e pedicure"
        ),
        Estabelecimento(
            "Pele & Beleza",
            "Estética",
            "Cuidados faciais e limpeza de pele"
        )
    )
    val categorias = listOf(
        "Todos", "Cabelo", "Unhas", "Estética"
    )

    val filtrados = estabelecimentos.filter { estabelecimento ->
        val combinaPesquisa = estabelecimento.nome.contains(
            pesquisa, ignoreCase = true
        ) || estabelecimento.descricao.contains(
            pesquisa, ignoreCase = true
        )

        val combinaCategoria =
            categoriaSelecionada == "Todos" ||
                    estabelecimento.categoria == categoriaSelecionada
        combinaPesquisa && combinaCategoria
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BelezaConectadaTheme {
        Greeting("Android")
    }
}