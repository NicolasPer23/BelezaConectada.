package com.example.belezaconectada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.belezaconectada.ui.viewmodel.ExplorarViewModel

@Composable
fun TelaExplorar(
    viewModel: ExplorarViewModel,
    categoriaSelecionadaParam: String,
    onLimparFiltro: () -> Unit
) {
    LaunchedEffect(categoriaSelecionadaParam) {
        if (categoriaSelecionadaParam.isNotEmpty()) {
            viewModel.updateCategoriaSelecionada(categoriaSelecionadaParam)
        }
    }

    var mostrarModalFiltro by remember { mutableStateOf(false) }

    val estabelecimentosFiltrados = viewModel.obterEstabelecimentosFiltrados()
    val categoriaSelecionada = viewModel.categoriaSelecionada
    val textoBusca = viewModel.textoBusca
    val filtroPreco = viewModel.filtroPreco
    val filtroDistancia = viewModel.filtroDistancia

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(20.dp)
    ) {
        Text("Explorar Estabelecimentos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textoBusca,
                onValueChange = { viewModel.updateTextoBusca(it) },
                label = { Text("Buscar serviços ou salões...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (textoBusca.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateTextoBusca("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = { mostrarModalFiltro = true },
                shape = RoundedCornerShape(14.dp),
                color = if (filtroPreco.isNotEmpty() || filtroDistancia.isNotEmpty() || categoriaSelecionada.isNotEmpty()) Color(0xFF8E4772) else Color(0xFFEEEEEE),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtros",
                        tint = if (filtroPreco.isNotEmpty() || filtroDistancia.isNotEmpty() || categoriaSelecionada.isNotEmpty()) Color.White else Color(0xFF8E4772)
                    )
                }
            }
        }

        if (categoriaSelecionada.isNotEmpty() || filtroPreco.isNotEmpty() || filtroDistancia.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (categoriaSelecionada.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF8E4772), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Categoria: $categoriaSelecionada", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.updateCategoriaSelecionada(""); onLimparFiltro() }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                if (filtroPreco.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF8E4772), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Preço: $filtroPreco", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.updateFiltroPreco("") }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                if (filtroDistancia.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF8E4772), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Até $filtroDistancia km", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.updateFiltroDistancia("") }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (estabelecimentosFiltrados.isEmpty()) {
            Text("Nenhum resultado encontrado no momento.", color = Color.Gray, fontSize = 14.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(estabelecimentosFiltrados.size) { index ->
                    val est = estabelecimentosFiltrados[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            if (est.imagemUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = est.imagemUrl,
                                    contentDescription = est.nome,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = est.nome, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
                                    Text(text = est.preco, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E4772))
                                }
                                Text(text = "${est.subcategoria} • ${est.distancia}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${est.avaliacao} (${est.avaliacoesCount})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarModalFiltro) {
        AlertDialog(
            onDismissRequest = { mostrarModalFiltro = false },
            title = { Text("Filtrar Busca", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Faixa de Preço", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("$$", "$$$", "$$$$").forEach { preco ->
                            FilterChip(
                                selected = filtroPreco == preco,
                                onClick = { viewModel.updateFiltroPreco(if (filtroPreco == preco) "" else preco) },
                                label = { Text(preco) }
                            )
                        }
                    }

                    Text("Distância Máxima", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1.0", "2.0", "5.0").forEach { dist ->
                            FilterChip(
                                selected = filtroDistancia == dist,
                                onClick = { viewModel.updateFiltroDistancia(if (filtroDistancia == dist) "" else dist) },
                                label = { Text("Até $dist km") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarModalFiltro = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772))
                ) {
                    Text("Aplicar Filtros")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.limparFiltros()
                        onLimparFiltro()
                        mostrarModalFiltro = false
                    }
                ) {
                    Text("Limpar Tudo", color = Color.Gray)
                }
            }
        )
    }
}
