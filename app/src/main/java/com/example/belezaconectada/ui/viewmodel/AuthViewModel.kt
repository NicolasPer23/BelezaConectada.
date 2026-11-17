package com.example.belezaconectada.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.belezaconectada.data.repository.AuthRepository
import com.example.belezaconectada.data.repository.BrasilApiRepository
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val brasilApiRepository: BrasilApiRepository = BrasilApiRepository()
) : ViewModel() {

    var email by mutableStateOf("")
    var senha by mutableStateOf("")
    var nome by mutableStateOf("")
    var cnpj by mutableStateOf("")
    var cep by mutableStateOf("")
    var isCadastro by mutableStateOf(false)
    var mensagemErro by mutableStateOf("")
    var carregando by mutableStateOf(false)
    var buscandoCnpj by mutableStateOf(false)
    var buscandoCep by mutableStateOf(false)

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun resetMensagemErro() {
        mensagemErro = ""
    }

    fun consultarCnpjSeValido(onSucesso: (nome: String) -> Unit) {
        val cnpjLimpo = cnpj.filter { it.isDigit() }
        if (cnpjLimpo.length == 14) {
            buscandoCnpj = true
            mensagemErro = ""
            viewModelScope.launch {
                try {
                    val resposta = brasilApiRepository.consultarCnpj(cnpjLimpo)
                    buscandoCnpj = false
                    val nomeEncontrado = resposta.nome_fantasia.takeIf { !it.isNullOrBlank() }
                        ?: resposta.razao_social ?: ""

                    if (nomeEncontrado.isNotEmpty()) {
                        nome = nomeEncontrado
                        onSucesso(nomeEncontrado)
                    }

                    val situacao = resposta.descricao_situacao_cadastral ?: ""
                    if (situacao.isNotEmpty() && !situacao.equals("ATIVA", ignoreCase = true)) {
                        mensagemErro = "Atenção: Situação do CNPJ na Receita Federal é $situacao."
                    }
                } catch (_: Exception) {
                    buscandoCnpj = false
                    mensagemErro = "CNPJ não encontrado na Receita Federal."
                }
            }
        }
    }

    fun consultarCepSeValido(onSucesso: (enderecoCompleto: String) -> Unit) {
        val cepLimpo = cep.filter { it.isDigit() }
        if (cepLimpo.length == 8) {
            buscandoCep = true
            mensagemErro = ""
            viewModelScope.launch {
                try {
                    val respCep = brasilApiRepository.consultarCep(cepLimpo)
                    buscandoCep = false
                    val rua = respCep.street ?: ""
                    val bairro = respCep.neighborhood ?: ""
                    val cidade = respCep.city ?: ""
                    val estado = respCep.state ?: ""
                    onSucesso("Endereço: $rua, $bairro - $cidade/$estado")
                } catch (_: Exception) {
                    buscandoCep = false
                    mensagemErro = "CEP não encontrado ou erro de conexão."
                }
            }
        }
    }

    fun executarAuth(
        isEstabelecimento: Boolean,
        onAguardandoVerificacao: (String) -> Unit,
        onLoginSucesso: () -> Unit
    ) {
        val emailValido = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val temOitoDigitos = senha.length >= 8
        val temMaiuscula = senha.any { it.isUpperCase() }
        val temMinuscula = senha.any { it.isLowerCase() }
        val temEspecial = senha.any { !it.isLetterOrDigit() }

        if (!emailValido) {
            mensagemErro = "Insira um e-mail válido."
            return
        }

        if (isCadastro) {
            if (nome.isBlank()) {
                mensagemErro = "Por favor, preencha o seu nome."
                return
            }
            if (isEstabelecimento && cnpj.length < 14) {
                mensagemErro = "Por favor, insira um CNPJ válido."
                return
            }
            if (!temOitoDigitos || !temMaiuscula || !temMinuscula || !temEspecial) {
                mensagemErro = "Senha fraca. Siga os requisitos solicitados."
                return
            }
        }

        carregando = true
        mensagemErro = ""

        if (isCadastro) {
            authRepository.cadastrarUsuario(
                email = email,
                senha = senha,
                nome = nome,
                isEstabelecimento = isEstabelecimento,
                cnpj = cnpj
            ) { sucesso, erro ->
                carregando = false
                if (sucesso) {
                    onAguardandoVerificacao(email)
                } else {
                    mensagemErro = erro ?: "Erro ao criar conta."
                }
            }
        } else {
            authRepository.signInWithEmail(email, senha) { sucesso, erro ->
                carregando = false
                if (sucesso) {
                    onLoginSucesso()
                } else {
                    mensagemErro = erro ?: "Credenciais inválidas."
                }
            }
        }
    }

    fun autenticarComGoogle(
        credential: AuthCredential,
        onLoginSucesso: () -> Unit
    ) {
        carregando = true
        authRepository.autenticarComGoogle(credential) { sucesso, erro ->
            carregando = false
            if (sucesso) {
                onLoginSucesso()
            } else {
                mensagemErro = erro ?: "Falha ao autenticar com Google."
            }
        }
    }

    fun checarEmailVerificado(onVerificado: () -> Unit) {
        carregando = true
        authRepository.checarVerificacaoEmail { verificado, erro ->
            carregando = false
            if (verificado) {
                onVerificado()
            } else {
                mensagemErro = erro ?: "E-mail ainda não verificado."
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
