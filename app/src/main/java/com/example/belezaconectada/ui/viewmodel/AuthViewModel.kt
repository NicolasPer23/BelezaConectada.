package com.example.belezaconectada.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.belezaconectada.data.repository.AuthRepository
import com.example.belezaconectada.data.repository.BrasilApiRepository
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val brasilApiRepository: BrasilApiRepository = BrasilApiRepository()
) : ViewModel() {

    var email by mutableStateOf("")
    var senha by mutableStateOf("")
    var confirmarSenha by mutableStateOf("")
    var nome by mutableStateOf("")
    var sobrenome by mutableStateOf("")
    var telefone by mutableStateOf("")
    var dataNascimento by mutableStateOf("")
    var genero by mutableStateOf("")
    var cnpj by mutableStateOf("")
    var cep by mutableStateOf("")
    var aceitouTermos by mutableStateOf(false)
    var isCadastro by mutableStateOf(false)
    var mensagemErro by mutableStateOf("")
    var carregando by mutableStateOf(false)
    var buscandoCnpj by mutableStateOf(false)
    var buscandoCep by mutableStateOf(false)

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun resetMensagemErro() {
        mensagemErro = ""
    }

    fun redefinirSenha(onSucesso: () -> Unit) {
        val emailValido = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        if (!emailValido) {
            mensagemErro = "Insira um e-mail válido no campo de e-mail para redefinir sua senha."
            return
        }
        carregando = true
        mensagemErro = ""
        authRepository.redefinirSenha(email) { sucesso, erro ->
            carregando = false
            if (sucesso) {
                onSucesso()
            } else {
                mensagemErro = erro ?: "Erro ao enviar e-mail de redefinição."
            }
        }
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
                } catch (_: UnknownHostException) {
                    buscandoCnpj = false
                    mensagemErro = "Sem conexão com o servidor da Receita Federal. Verifique sua internet."
                } catch (_: IOException) {
                    buscandoCnpj = false
                    mensagemErro = "Erro de conexão de rede ao buscar CNPJ."
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
                } catch (_: UnknownHostException) {
                    buscandoCep = false
                    mensagemErro = "Sem conexão de rede ao consultar CEP."
                } catch (_: IOException) {
                    buscandoCep = false
                    mensagemErro = "Erro de rede. Verifique sua conexão de internet."
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
            if (sobrenome.isBlank()) {
                mensagemErro = "Por favor, preencha o seu sobrenome."
                return
            }
            if (telefone.isBlank()) {
                mensagemErro = "Por favor, informe seu número de telefone."
                return
            }
            if (dataNascimento.isBlank()) {
                mensagemErro = "Por favor, informe sua data de nascimento."
                return
            }
            if (genero.isBlank()) {
                mensagemErro = "Por favor, selecione seu sexo (Feminino ou Masculino)."
                return
            }
            if (isEstabelecimento && cnpj.length < 14) {
                mensagemErro = "Por favor, insira um CNPJ válido."
                return
            }
            if (senha != confirmarSenha) {
                mensagemErro = "As senhas não coincidem. Digite novamente."
                return
            }
            if (!temOitoDigitos || !temMaiuscula || !temMinuscula || !temEspecial) {
                mensagemErro = "Senha fraca. A senha deve ter 8+ caracteres, com maiúscula, minúscula e caractere especial."
                return
            }
            if (!aceitouTermos) {
                mensagemErro = "Por favor, aceite os termos de autorização para continuar."
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
                sobrenome = sobrenome,
                telefone = telefone,
                dataNascimento = dataNascimento,
                genero = genero,
                isEstabelecimento = isEstabelecimento,
                cnpj = cnpj,
                aceitouTermos = aceitouTermos
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
