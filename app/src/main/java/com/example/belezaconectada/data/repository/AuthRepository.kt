package com.example.belezaconectada.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    fun signOut() {
        auth.signOut()
    }

    fun signInWithEmail(
        email: String,
        senha: String,
        onResult: (sucesso: Boolean, mensagemErro: String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        onResult(true, null)
                    } else {
                        auth.signOut()
                        onResult(false, "Por favor, verifique o seu e-mail antes de entrar.")
                    }
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Credenciais inválidas.")
                }
            }
    }

    fun redefinirSenha(
        email: String,
        onResult: (sucesso: Boolean, mensagemErro: String?) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Erro ao enviar e-mail de redefinição de senha.")
                }
            }
    }

    fun cadastrarUsuario(
        email: String,
        senha: String,
        nome: String,
        sobrenome: String = "",
        telefone: String = "",
        dataNascimento: String = "",
        genero: String = "",
        isEstabelecimento: Boolean,
        cnpj: String,
        aceitouTermos: Boolean = true,
        onResult: (sucesso: Boolean, mensagemErro: String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    val dadosUsuario = hashMapOf(
                        "uid" to userId,
                        "nome" to nome,
                        "sobrenome" to sobrenome,
                        "nomeCompleto" to "$nome $sobrenome".trim(),
                        "telefone" to telefone,
                        "dataNascimento" to dataNascimento,
                        "genero" to genero,
                        "email" to email,
                        "tipoPerfil" to if (isEstabelecimento) "estabelecimento" else "cliente",
                        "cnpj" to if (isEstabelecimento) cnpj else "",
                        "aceitouTermos" to aceitouTermos,
                        "statusAprovacao" to if (isEstabelecimento) "PENDENTE" else "APROVADO",
                        "criadoEm" to System.currentTimeMillis()
                    )

                    db.collection("usuarios").document(userId)
                        .set(dadosUsuario)
                        .addOnSuccessListener {
                            auth.currentUser?.sendEmailVerification()
                            onResult(true, null)
                        }
                        .addOnFailureListener { e ->
                            onResult(false, "Erro ao guardar dados no banco: ${e.localizedMessage}")
                        }
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Erro ao criar conta.")
                }
            }
    }

    fun checarVerificacaoEmail(
        onResult: (verificado: Boolean, mensagemErro: String?) -> Unit
    ) {
        auth.currentUser?.reload()?.addOnCompleteListener {
            if (auth.currentUser?.isEmailVerified == true) {
                onResult(true, null)
            } else {
                onResult(false, "E-mail ainda não verificado. Por favor, cheque sua caixa de entrada.")
            }
        } ?: onResult(false, "Nenhum usuário conectado.")
    }
}
