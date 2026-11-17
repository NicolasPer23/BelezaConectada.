package com.example.belezaconectada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaAgendamentos(
    isLogado: Boolean,
    onLoginClick: () -> Unit,
    onExplorar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(Color(0xFFEEEEEE), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Meus Agendamentos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(8.dp))
        if (!isLogado) {
            Text(
                text = "Você precisa estar conectado para visualizar seus agendamentos.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Entrar ou Cadastrar", color = Color.White)
            }
        } else {
            Text(
                text = "Você ainda não possui agendamentos realizados.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onExplorar,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Explorar Serviços", color = Color.White)
            }
        }
    }
}
