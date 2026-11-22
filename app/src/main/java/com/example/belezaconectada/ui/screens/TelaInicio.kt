package com.example.belezaconectada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.belezaconectada.data.model.Estabelecimento

@Composable
fun TelaInicio(
    localizacaoAtual: String,
    estabelecimentos: List<Estabelecimento> = emptyList(),
    onExp: () -> Unit,
    onCategoriaClick: (String) -> Unit,
    onEstabelecimentoClick: (Estabelecimento) -> Unit,
    onLoginClick: () -> Unit,
    isLogado: Boolean
) {
    val categorias = listOf(
        Pair("Cabelos", Icons.Default.Face3),
        Pair("Unhas", Icons.Default.Brush),
        Pair("Barbearia", Icons.Default.ContentCut),
        Pair("Estética & Massagem", Icons.Default.Spa),
        Pair("Sobrancelha", Icons.Default.VisibilityOff),
        Pair("Maquiagem", Icons.Default.Palette),
        Pair("Depilação", Icons.Default.CleanHands),
        Pair("Outros Serviços", Icons.Default.AutoAwesome)
    )

    val linhasDeCategorias = categorias.chunked(4)

    val melhoresAvaliados = estabelecimentos.sortedByDescending {
        it.avaliacao.toDoubleOrNull() ?: 0.0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9FA))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "GPS",
                        tint = Color(0xFF8E4772),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = localizacaoAtual,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF2D2D2D)
                    )
                }
                if (!isLogado) {
                    TextButton(onClick = onLoginClick) {
                        Text("Entrar / Cadastrar", color = Color(0xFF8E4772), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("Beleza Conectada ✨", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
            Text("Encontre os melhores serviços perto de você", fontSize = 12.sp, color = Color.Gray)
        }

        item {
            Text("Categorias", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E4772))
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                linhasDeCategorias.forEach { linha ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        linha.forEach { (nome, icone) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCategoriaClick(nome) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color(0xFFFFF0F5), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icone,
                                        contentDescription = nome,
                                        tint = Color(0xFF8E4772),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = nome,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2D2D2D),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Seção: Recomendados (layout mais estético e discreto)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recomendados para Você", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E4772))
                TextButton(onClick = onExp) {
                    Text("Ver todos", color = Color(0xFF8E4772), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(melhoresAvaliados) { est ->
            CardMelhorAvaliado(
                estabelecimento = est,
                onClick = { onEstabelecimentoClick(est) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onExp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Explorar Todos os Serviços", color = Color(0xFF8E4772), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CardMelhorAvaliado(
    estabelecimento: Estabelecimento,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            if (estabelecimento.imagemUrl.isNotEmpty()) {
                AsyncImage(
                    model = estabelecimento.imagemUrl,
                    contentDescription = estabelecimento.nome,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = estabelecimento.nome,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = estabelecimento.preco,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E4772)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${estabelecimento.subcategoria} • ${estabelecimento.distancia}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${estabelecimento.avaliacao} (${estabelecimento.avaliacoesCount})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2D2D)
                        )
                    }
                    Surface(
                        color = Color(0xFFFFF0F5),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = estabelecimento.status,
                            color = Color(0xFF8E4772),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
