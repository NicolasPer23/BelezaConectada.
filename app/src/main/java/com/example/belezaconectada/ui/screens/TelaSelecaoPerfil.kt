package com.example.belezaconectada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaSelecaoPerfil(
    onIrParaLoginCliente: () -> Unit,
    onIrParaLoginEstabelecimento: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Como deseja continuar?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onIrParaLoginCliente,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Sou Cliente", fontSize = 16.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onIrParaLoginEstabelecimento,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Sou Estabelecimento", fontSize = 16.sp, color = Color(0xFF8E4772))
        }
    }
}
