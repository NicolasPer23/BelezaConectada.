package com.example.belezaconectada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaVerificacaoCnpj(onVoltar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = Color(0xFF8E4772), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Verificação de CNPJ", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(8.dp))
        Text("O seu cadastro de estabelecimento está em análise de CNPJ.", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onVoltar,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Voltar para o Início", color = Color.White)
        }
    }
}
