package com.example.belezaconectada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaInicio(
    localizacaoAtual: String,
    onExp: () -> Unit,
    onCategoriaClick: (String) -> Unit,
    onDet: () -> Unit,
    onLoginClick: () -> Unit,
    isLogado: Boolean
) {
    val categorias = listOf(
        Pair("Cabelo", Icons.Default.ContentCut),
        Pair("Unhas", Icons.Default.Brush),
        Pair("Barba", Icons.Default.Face),
        Pair("Estética", Icons.Default.AutoAwesome),
        Pair("Sobrancelha", Icons.Default.Visibility),
        Pair("Maquiagem", Icons.Default.Palette),
        Pair("Massagem", Icons.Default.Spa),
        Pair("Academia", Icons.Default.FitnessCenter)
    )

    val linhasDeCategorias = categorias.chunked(4)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GPS: $localizacaoAtual", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (!isLogado) {
                    TextButton(onClick = onLoginClick) {
                        Text("Entrar / Cadastrar", color = Color(0xFF8E4772), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Beleza Conectada!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Categorias", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E4772))
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                linhasDeCategorias.forEach { linha ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        linha.forEach { (nome, icone) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { onCategoriaClick(nome) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFFFF0F5), shape = CircleShape)
                                ) {
                                    Icon(
                                        imageVector = icone,
                                        contentDescription = nome,
                                        tint = Color(0xFF8E4772),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = nome,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2D2D2D)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDet,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772))
            ) {
                Text("Ver Exemplo: Studio Aura")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onExp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Explorar Serviços", color = Color(0xFF8E4772))
            }
        }
    }
}
