package com.example.belezaconectada.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.belezaconectada.R
import com.example.belezaconectada.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun TelaLoginCadastro(
    titulo: String,
    isEstabelecimento: Boolean,
    viewModel: AuthViewModel,
    onAguardandoVerificacao: (String) -> Unit,
    onLoginSucesso: () -> Unit,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current

    // Efeito automático para consultar o CNPJ ao digitar 14 dígitos
    LaunchedEffect(viewModel.cnpj) {
        if (viewModel.isCadastro && isEstabelecimento) {
            viewModel.consultarCnpjSeValido { nomeEncontrado ->
                Toast.makeText(context, "CNPJ verificado! Nome autopreenchido.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Efeito automático para consultar o CEP ao digitar 8 dígitos
    LaunchedEffect(viewModel.cep) {
        if (viewModel.isCadastro && isEstabelecimento) {
            viewModel.consultarCepSeValido { enderecoMsg ->
                Toast.makeText(context, enderecoMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launcher que recebe o resultado do clique no botão "Continuar com o Google"
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            viewModel.autenticarComGoogle(credential, onLoginSucesso)
        } catch (_: Exception) {
            viewModel.mensagemErro = "Login com o Google cancelado ou com erro."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onVoltar) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF8E4772))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (viewModel.isCadastro) "Criar conta - $titulo" else "Bem-vindo de volta!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D2D2D)
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (viewModel.isCadastro) {
            OutlinedTextField(
                value = viewModel.nome,
                onValueChange = { viewModel.nome = it },
                label = { Text("Nome completo / Razão Social") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isEstabelecimento) {
                OutlinedTextField(
                    value = viewModel.cnpj,
                    onValueChange = { viewModel.cnpj = it },
                    label = { Text("CNPJ do Estabelecimento (14 dígitos)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (viewModel.buscandoCnpj) {
                            CircularProgressIndicator(color = Color(0xFF8E4772), modifier = Modifier.size(20.dp))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.cep,
                    onValueChange = { viewModel.cep = it },
                    label = { Text("CEP do Endereço (8 dígitos)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (viewModel.buscandoCep) {
                            CircularProgressIndicator(color = Color(0xFF8E4772), modifier = Modifier.size(20.dp))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.senha,
            onValueChange = { viewModel.senha = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        if (viewModel.mensagemErro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = viewModel.mensagemErro, color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.executarAuth(
                    isEstabelecimento = isEstabelecimento,
                    onAguardandoVerificacao = onAguardandoVerificacao,
                    onLoginSucesso = onLoginSucesso
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
            shape = RoundedCornerShape(14.dp),
            enabled = !viewModel.carregando
        ) {
            if (viewModel.carregando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = if (viewModel.isCadastro) "Cadastrar com E-mail" else "Entrar com E-mail",
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        if (!isEstabelecimento) {
            Spacer(modifier = Modifier.height(12.dp))
            val webClientId = stringResource(id = R.string.default_web_client_id)
            OutlinedButton(
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    googleLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF8E4772))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Continuar com o Google", fontSize = 14.sp, color = Color(0xFF8E4772), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        TextButton(
            onClick = { viewModel.isCadastro = !viewModel.isCadastro; viewModel.resetMensagemErro() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = if (viewModel.isCadastro) "Já tem conta? Faça login" else "Não tem conta? Cadastre-se", color = Color(0xFF8E4772))
        }
    }
}
