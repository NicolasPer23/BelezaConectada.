package com.example.belezaconectada.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.belezaconectada.ui.viewmodel.AuthViewModel

@Composable
fun TelaAguardandoEmailVerificacao(
    email: String,
    viewModel: AuthViewModel,
    onVerificado: () -> Unit,
    onVoltar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF8E4772), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Verifique seu E-mail", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enviamos um e-mail de confirmação para:\n$email", color = Color.Gray, fontSize = 14.sp)

        if (viewModel.mensagemErro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = viewModel.mensagemErro, color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.checarEmailVerificado(onVerificado)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
            shape = RoundedCornerShape(14.dp),
            enabled = !viewModel.carregando
        ) {
            if (viewModel.carregando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Já verifiquei o e-mail", color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onVoltar) {
            Text("Voltar ao Login", color = Color(0xFF8E4772))
        }
    }
}
