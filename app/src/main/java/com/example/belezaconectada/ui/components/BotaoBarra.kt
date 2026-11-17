package com.example.belezaconectada.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BotaoBarra(titulo: String, icone: ImageVector, selecionado: Boolean, onClick: () -> Unit) {
    val corTema = if (selecionado) Color(0xFF8E4772) else Color.Gray
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icone, contentDescription = titulo, tint = corTema, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = titulo, fontSize = 10.sp, color = corTema, fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
