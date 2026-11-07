package com.example.belezaconectada

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.belezaconectada.ui.theme.BelezaConectadaTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.util.Locale

// --- MODELO DE DADOS ---
data class Estabelecimento(
    val id: String,
    val nome: String,
    val subcategoria: String,
    val categoria: String,
    val descricao: String,
    val tag: String,
    val avaliacao: String,
    val avaliacoesCount: String,
    val distancia: String,
    val status: String,
    val preco: String,
    var favorito: Boolean
)

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private var localizacaoAtual by mutableStateOf("Verificando localização...")
    private var gpsPermitido by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocation || coarseLocation) {
            gpsPermitido = true
            obterLocalizacaoGPS()
        } else {
            gpsPermitido = false
            localizacaoAtual = "Permissão negada. O app requer localização."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        enableEdgeToEdge()
        verificarPermissaoEObterGPS()

        setContent {
            BelezaConectadaTheme {
                val navController = rememberNavController()
                var tipoPerfil by remember { mutableStateOf("cliente") }
                var emailParaVerificar by remember { mutableStateOf("") }
                var telaAtualBarra by remember { mutableStateOf("inicio") }

                // O utilizador pode navegar como convidado caso não esteja logado
                val startDest = "inicio"

                if (!gpsPermitido && localizacaoAtual.contains("Permissão negada")) {
                    TelaBloqueioGPS(onTentarNovamente = { verificarPermissaoEObterGPS() })
                } else {
                    Scaffold(
                        bottomBar = {
                            if (tipoPerfil == "cliente") {
                                Surface(color = Color.White, shadowElevation = 8.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        BotaoBarra("Início", Icons.Default.Home, telaAtualBarra == "inicio") {
                                            telaAtualBarra = "inicio"
                                            navController.navigate("inicio") { popUpTo("inicio") { inclusive = true } }
                                        }
                                        BotaoBarra("Explorar", Icons.Default.Search, telaAtualBarra == "explorar") {
                                            telaAtualBarra = "explorar"
                                            navController.navigate("explorar") { popUpTo("inicio") }
                                        }
                                        BotaoBarra("Favoritos", Icons.Default.Favorite, telaAtualBarra == "favoritos") {
                                            if (auth.currentUser == null) {
                                                navController.navigate("selecao_perfil")
                                            } else {
                                                telaAtualBarra = "favoritos"
                                                navController.navigate("favoritos") { popUpTo("inicio") }
                                            }
                                        }
                                        BotaoBarra("Agenda", Icons.Default.DateRange, telaAtualBarra == "agendamentos") {
                                            if (auth.currentUser == null) {
                                                navController.navigate("selecao_perfil")
                                            } else {
                                                telaAtualBarra = "agendamentos"
                                                navController.navigate("agendamentos") { popUpTo("inicio") }
                                            }
                                        }
                                        BotaoBarra("Perfil", Icons.Default.Person, telaAtualBarra == "perfil") {
                                            if (auth.currentUser == null) {
                                                navController.navigate("selecao_perfil")
                                            } else {
                                                telaAtualBarra = "perfil"
                                                navController.navigate("perfil") { popUpTo("inicio") }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        containerColor = Color(0xFFFFF9FA)
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startDest,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("inicio") {
                                TelaInicio(
                                    localizacaoAtual = localizacaoAtual,
                                    onExp = { telaAtualBarra = "explorar"; navController.navigate("explorar") },
                                    onDet = { navController.navigate("detalhes") },
                                    onLoginClick = { navController.navigate("selecao_perfil") },
                                    isLogado = auth.currentUser != null
                                )
                            }
                            composable("explorar") { TelaExplorar() }
                            composable("favoritos") { TelaFavoritos() }
                            composable("agendamentos") { TelaAgendamentos { telaAtualBarra = "explorar"; navController.navigate("explorar") } }
                            composable("selecao_perfil") {
                                TelaSelecaoPerfil(
                                    onIrParaLoginCliente = { navController.navigate("login_cliente") },
                                    onIrParaLoginEstabelecimento = { navController.navigate("login_estabelecimento") }
                                )
                            }
                            composable("login_cliente") {
                                TelaLoginCadastro(
                                    titulo = "Acesso Cliente",
                                    isEstabelecimento = false,
                                    auth = auth,
                                    onAguardandoVerificacao = { emailDigitado ->
                                        emailParaVerificar = emailDigitado
                                        tipoPerfil = "cliente"
                                        navController.navigate("aguardando_email")
                                    },
                                    onLoginSucesso = {
                                        tipoPerfil = "cliente"
                                        telaAtualBarra = "inicio"
                                        navController.navigate("inicio") { popUpTo("inicio") { inclusive = true } }
                                    },
                                    onVoltar = { navController.popBackStack() }
                                )
                            }
                            composable("login_estabelecimento") {
                                TelaLoginCadastro(
                                    titulo = "Acesso Estabelecimento",
                                    isEstabelecimento = true,
                                    auth = auth,
                                    onAguardandoVerificacao = { emailDigitado ->
                                        emailParaVerificar = emailDigitado
                                        tipoPerfil = "estabelecimento"
                                        navController.navigate("aguardando_email")
                                    },
                                    onLoginSucesso = {
                                        tipoPerfil = "estabelecimento"
                                        navController.navigate("verificacao_cnpj") { popUpTo("inicio") { inclusive = true } }
                                    },
                                    onVoltar = { navController.popBackStack() }
                                )
                            }
                            composable("aguardando_email") {
                                TelaAguardandoEmailVerificacao(
                                    email = emailParaVerificar,
                                    auth = auth,
                                    onVerificado = {
                                        if (tipoPerfil == "cliente") {
                                            telaAtualBarra = "inicio"
                                            navController.navigate("inicio") { popUpTo("inicio") { inclusive = true } }
                                        } else {
                                            navController.navigate("verificacao_cnpj") { popUpTo("inicio") { inclusive = true } }
                                        }
                                    },
                                    onVoltar = { navController.popBackStack() }
                                )
                            }
                            composable("verificacao_cnpj") {
                                TelaVerificacaoCnpj(
                                    onVoltar = { navController.navigate("selecao_perfil") { popUpTo(0) } }
                                )
                            }
                            composable("perfil") {
                                TelaPerfil {
                                    auth.signOut()
                                    telaAtualBarra = "inicio"
                                    navController.navigate("inicio") { popUpTo(0) }
                                }
                            }
                            composable("detalhes") {
                                TelaDetalhesEstabelecimento(
                                    nome = "Studio Aura",
                                    sub = "Salão de beleza",
                                    av = "4,9",
                                    onVoltar = { navController.popBackStack() }
                                ) {
                                    if (auth.currentUser == null) {
                                        navController.navigate("selecao_perfil")
                                    } else {
                                        telaAtualBarra = "agendamentos"
                                        navController.navigate("agendamentos") { popUpTo("inicio") }
                                    }
                                }
                            }
                            composable("painel_estabelecimento") {
                                TelaPainelEstabelecimento { tipoPerfil = "cliente"; telaAtualBarra = "inicio"; navController.navigate("inicio") { popUpTo("painel_estabelecimento") { inclusive = true } } }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun verificarPermissaoEObterGPS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gpsPermitido = true
            obterLocalizacaoGPS()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun obterLocalizacaoGPS() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val bairro = addresses[0].subLocality ?: addresses[0].thoroughfare ?: "Local atual"
                        val cidade = addresses[0].adminArea ?: ""
                        localizacaoAtual = "$bairro,$cidade"
                    } else {
                        localizacaoAtual = "São Paulo, SP"
                    }
                } else {
                    localizacaoAtual = "São Paulo, SP"
                }
            }
        } catch (e: SecurityException) {
            localizacaoAtual = "São Paulo, SP"
        }
    }
}

// --- BOTÃO DA BARRA DE NAVEGAÇÃO ---
@Composable
fun BotaoBarra(titulo: String, icone: androidx.compose.ui.graphics.vector.ImageVector, selecionado: Boolean, onClick: () -> Unit) {
    val corTema = if (selecionado) Color(0xFF8E4772) else Color.Gray
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icone, contentDescription = titulo, tint = corTema, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = titulo, fontSize = 10.sp, color = corTema, fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

// --- TELA INICIAL (COM SUPORTE A VISITANTE / LOGADO) ---
@Composable
fun TelaInicio(
    localizacaoAtual: String,
    onExp: () -> Unit,
    onDet: () -> Unit,
    onLoginClick: () -> Unit,
    isLogado: Boolean
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GPS: $localizacaoAtual", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (!isLogado) {
                    TextButton(onClick = onLoginClick) {
                        Text("Entrar / Cadastrar", color = Color(0xFF8E4772), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Bem-vindo ao Beleza Conectada!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDet,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772))
            ) {
                Text("Ver Exemplo: Studio Aura")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onExp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Explorar Serviços", color = Color(0xFF8E4772))
            }
        }
    }
}

// --- TELA DE LOGIN / CADASTRO COM GOOGLE ---
@Composable
fun TelaLoginCadastro(
    titulo: String,
    isEstabelecimento: Boolean,
    auth: FirebaseAuth,
    onAguardandoVerificacao: (String) -> Unit,
    onLoginSucesso: () -> Unit,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var cnpj by remember { mutableStateOf("") }
    var isCadastro by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            carregando = true
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    carregando = false
                    if (authTask.isSuccessful) {
                        onLoginSucesso()
                    } else {
                        mensagemErro = authTask.exception?.localizedMessage ?: "Falha ao autenticar com Google."
                    }
                }
        } catch (e: Exception) {
            carregando = false
            mensagemErro = "Login com o Google cancelado ou com erro."
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp), verticalArrangement = Arrangement.Center) {
        IconButton(onClick = onVoltar) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF8E4772))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = if (isCadastro) "Criar conta - $titulo" else "Bem-vindo de volta!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(20.dp))

        if (isCadastro) {
            OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            if (isEstabelecimento) {
                OutlinedTextField(value = cnpj, onValueChange = { cnpj = it }, label = { Text("CNPJ do Estabelecimento") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha (Mín. 8 char, 1 maiúscula, 1 especial)") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)

        if (mensagemErro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mensagemErro, color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val emailValido = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                val temOitoDigitos = senha.length >= 8
                val temMaiuscula = senha.any { it.isUpperCase() }
                val temMinuscula = senha.any { it.isLowerCase() }
                val temEspecial = senha.any { !it.isLetterOrDigit() }

                if (!emailValido) {
                    mensagemErro = "Insira um e-mail válido."
                    return@Button
                }

                if (isCadastro && (!temOitoDigitos || !temMaiuscula || !temMinuscula || !temEspecial)) {
                    mensagemErro = "Senha fraca. Siga os requisitos solicitados."
                    return@Button
                }

                carregando = true
                mensagemErro = ""

                if (isCadastro) {
                    auth.createUserWithEmailAndPassword(email, senha)
                        .addOnCompleteListener { task ->
                            carregando = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        onAguardandoVerificacao(email)
                                    } else {
                                        Toast.makeText(context, "Erro ao enviar e-mail de verificação.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                mensagemErro = task.exception?.localizedMessage ?: "Erro ao criar conta."
                            }
                        }
                } else {
                    auth.signInWithEmailAndPassword(email, senha)
                        .addOnCompleteListener { task ->
                            carregando = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null && user.isEmailVerified) {
                                    onLoginSucesso()
                                } else {
                                    mensagemErro = "Por favor, verifique o seu e-mail antes de entrar."
                                    auth.signOut()
                                }
                            } else {
                                mensagemErro = task.exception?.localizedMessage ?: "Credenciais inválidas."
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
            shape = RoundedCornerShape(14.dp),
            enabled = !carregando
        ) {
            if (carregando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(text = if (isCadastro) "Cadastrar com E-mail" else "Entrar com E-mail", fontSize = 15.sp, color = Color.White)
            }
        }

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

        TextButton(onClick = { isCadastro = !isCadastro; mensagemErro = "" }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text = if (isCadastro) "Já tem conta? Faça login" else "Não tem conta? Cadastre-se", color = Color(0xFF8E4772))
        }
    }
}

// --- OUTRAS TELAS ---
@Composable
fun TelaBloqueioGPS(onTentarNovamente: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.LocationOff, contentDescription = null, tint = Color(0xFF8E4772), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Localização Obrigatória", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "O aplicativo precisa do acesso à sua localização para listar estabelecimentos próximos.",
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onTentarNovamente,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Tentar Novamente", color = Color.White)
        }
    }
}

@Composable
fun TelaExplorar() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(20.dp)
    ) {
        Text("Explorar Estabelecimentos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Buscar serviços ou salões...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Nenhum resultado encontrado no momento.", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun TelaFavoritos() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(20.dp)
    ) {
        Text("Meus Favoritos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Você ainda não salvou nenhum estabelecimento como favorito.", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun TelaAgendamentos(onExplorar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF8E4772), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Meus Agendamentos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Você ainda não possui agendamentos realizados.", color = Color.Gray, fontSize = 14.sp)
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

@Composable
fun TelaAguardandoEmailVerificacao(
    email: String,
    auth: FirebaseAuth,
    onVerificado: () -> Unit,
    onVoltar: () -> Unit
) {
    var mensagemErro by remember { mutableStateOf("") }
    var checando by remember { mutableStateOf(false) }

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

        if (mensagemErro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mensagemErro, color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                checando = true
                auth.currentUser?.reload()?.addOnCompleteListener {
                    checando = false
                    if (auth.currentUser?.isEmailVerified == true) {
                        onVerificado()
                    } else {
                        mensagemErro = "E-mail ainda não verificado. Por favor, cheque sua caixa de entrada."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
            shape = RoundedCornerShape(14.dp),
            enabled = !checando
        ) {
            if (checando) {
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

@Composable
fun TelaPerfil(onSair: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp)
    ) {
        Text("Meu Perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSair,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Sair da Conta", color = Color.White)
        }
    }
}

@Composable
fun TelaDetalhesEstabelecimento(
    nome: String,
    sub: String,
    av: String,
    onVoltar: () -> Unit,
    onAgendar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(20.dp)
    ) {
        IconButton(onClick = onVoltar) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF8E4772))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(nome, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Text(sub, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(av, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAgendar,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Agendar Horário", color = Color.White)
        }
    }
}

@Composable
fun TelaPainelEstabelecimento(onVoltar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp)
    ) {
        IconButton(onClick = onVoltar) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF8E4772))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Painel do Estabelecimento", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gerencie seus serviços e agendamentos recebidos.", color = Color.Gray, fontSize = 14.sp)
    }
}

