package com.example.belezaconectada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaPainelEstabelecimento(onVoltar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp)
    ) {
        IconButton(onClick = onVoltar) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF8E4772))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Painel do Estabelecimento", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gerencie seus serviços e agendamentos recebidos.", color = Color.Gray, fontSize = 14.sp)
    }
}
