package com.example.belezaconectada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaDetalhesEstabelecimento(
    nome: String,
    sub: String,
    av: String,
    onVoltar: () -> Unit,
    onAgendar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(20.dp)
    ) {
        IconButton(onClick = onVoltar) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF8E4772))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(nome, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Text(sub, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(av, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAgendar,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Agendar Horário", color = Color.White)
        }
    }
}
