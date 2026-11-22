package com.example.belezaconectada.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.belezaconectada.ui.viewmodel.AuthViewModel

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
    val scrollState = rememberScrollState()

    // Efeito automático para consultar o CNPJ ao digitar 14 dígitos
    LaunchedEffect(viewModel.cnpj) {
        if (viewModel.isCadastro && isEstabelecimento) {
            viewModel.consultarCnpjSeValido { _ ->
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9FA))
            .padding(24.dp)
            .verticalScroll(scrollState),
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
            // Linha 1: Nome
            OutlinedTextField(
                value = viewModel.nome,
                onValueChange = { viewModel.nome = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Linha 2: Sobrenome
            OutlinedTextField(
                value = viewModel.sobrenome,
                onValueChange = { viewModel.sobrenome = it },
                label = { Text("Sobrenome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Linha 3: Telefone
            OutlinedTextField(
                value = viewModel.telefone,
                onValueChange = { viewModel.telefone = it },
                label = { Text("Número de Telefone / Celular") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Linha 4: Data de Aniversário
            OutlinedTextField(
                value = viewModel.dataNascimento,
                onValueChange = { viewModel.dataNascimento = it },
                label = { Text("Data de Aniversário (DD/MM/AAAA)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Seleção de Sexo / Gênero
            Text(text = "Sexo / Gênero:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D2D2D))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = { viewModel.genero = "Feminino" },
                    shape = RoundedCornerShape(20.dp),
                    color = if (viewModel.genero == "Feminino") Color(0xFF8E4772) else Color(0xFFFFF0F5),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Feminino",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.genero == "Feminino") Color.White else Color(0xFF8E4772)
                        )
                    }
                }

                Surface(
                    onClick = { viewModel.genero = "Masculino" },
                    shape = RoundedCornerShape(20.dp),
                    color = if (viewModel.genero == "Masculino") Color(0xFF8E4772) else Color(0xFFFFF0F5),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Masculino",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.genero == "Masculino") Color.White else Color(0xFF8E4772)
                        )
                    }
                }
            }
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
                Spacer(modifier = Modifier.height(10.dp))

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
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = viewModel.senha,
            onValueChange = { viewModel.senha = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        if (!viewModel.isCadastro) {
            TextButton(
                onClick = {
                    viewModel.redefinirSenha {
                        Toast.makeText(context, "E-mail de redefinição enviado para: ${viewModel.email}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "Esqueceu a senha?", fontSize = 12.sp, color = Color(0xFF8E4772), fontWeight = FontWeight.SemiBold)
            }
        }

        if (viewModel.isCadastro) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = viewModel.confirmarSenha,
                onValueChange = { viewModel.confirmarSenha = it },
                label = { Text("Confirmar Senha") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Termos de Uso e Autorização
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.aceitouTermos = !viewModel.aceitouTermos }
            ) {
                Checkbox(
                    checked = viewModel.aceitouTermos,
                    onCheckedChange = { viewModel.aceitouTermos = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF8E4772))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Li e aceito os Termos de Uso e autorizo a coleta das informações para o funcionamento do aplicativo.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp
                )
            }
        }

        if (viewModel.mensagemErro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = viewModel.mensagemErro, color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = { viewModel.isCadastro = !viewModel.isCadastro; viewModel.resetMensagemErro() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = if (viewModel.isCadastro) "Já tem conta? Faça login" else "Não tem conta? Cadastre-se", color = Color(0xFF8E4772))
        }
    }
}
