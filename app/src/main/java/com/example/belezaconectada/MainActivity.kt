package com.example.belezaconectada

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.belezaconectada.ui.theme.BelezaConectadaTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.Locale

// --- MODELO E SERVIÇO DA BRASILAPI (CONSULTA DE CNPJ E CEP) ---
data class CnpjResposta(
    val cnpj: String? = null,
    val razao_social: String? = null,
    val nome_fantasia: String? = null,
    val descricao_situacao_cadastral: String? = null
)

data class CepResposta(
    val cep: String? = null,
    val state: String? = null,
    val city: String? = null,
    val neighborhood: String? = null,
    val street: String? = null
)

interface BrasilApiService {
    @GET("api/cnpj/v1/{cnpj}")
    suspend fun consultarCnpj(@Path("cnpj") cnpj: String): CnpjResposta

    @GET("api/cep/v1/{cep}")
    suspend fun consultarCep(@Path("cep") cep: String): CepResposta
}

object RetrofitClient {
    val brasilApi: BrasilApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://brasilapi.com.br/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BrasilApiService::class.java)
    }
}

//Classe de dados que representa as propriedades de um salão/barbearia/estética
data class Estabelecimento(
    val id: String, //Identificador único no banco
    val nome: String, // Nome do estabelecimento (ex: "Studio Aura")
    val subcategoria: String, // Subcategoria (ex: "Salão de Beleza")
    val categoria: String, // Categoria Principal (ex: "Cabelo")
    val descricao: String, // Descrição textual do local
    val tag: String, // Etiqueta de Busca
    val avaliacao: String, //Nota de Avaliação
    val avaliacoesCount: String,// Quantidade de avaliações recebidas
    val distancia: String, // Distancia do utilizador (ex: "1,2km")
    val status: String, // Ex: "Aberto Agora"
    val preco: String, // Faixa de preço (ex "$$")
    var favorito: Boolean, // define se o utilizador favoritou este salão (true ou false)
    val imagemUrl: String = "" // URL da imagem da fachada do estabelecimento
)

class MainActivity : ComponentActivity() { // Activity principal que roda o app no celular

    private lateinit var auth: FirebaseAuth // Instância do firabaseAuth para verificar se o utlizador está logado
    private var localizacaoAtual by mutableStateOf("Verificando localização...") //Estado reativo: quando muda, o Compose atualiza o texto na tela
    private var gpsPermitido by mutableStateOf(false) // Guarda se o utilizador de permissão o acesso ao GPS

    //Registra um ouvinte para capturar a resposta da caixa de diálogo de permissão do Android:
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions() //Pede multiplas permissões ao mesmo tempo
    ) { permissions ->
        // Verifica se a permissão de GPS preciso ou aproximado foi concedida:
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false //(?:) "Procure a permissão de GPS no mapa. Se ela existir, me de o valor dela. SE FOR NULA (null), use o valor padrão false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocation || coarseLocation) {
            gpsPermitido = true
            obterLocalizacaoGPS() // se aceitou, busca o GPS imediatamente
        } else {
            gpsPermitido = false
            localizacaoAtual = "Permissão negada. O app requer localização." // se negou exibe a mensagem
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance() //Inicializa a conexão com o Firebase Auth
        enableEdgeToEdge() // Faz a tela ocupar 100% da área do visor (atrás da barra de status e de navegação
        verificarPermissaoEObterGPS() // Dispara a checagem de permissões ao iniciar o app

        setContent { //Define que o conteúdo da tela será construído via jetpack Compose
            BelezaConectadaTheme { // Aplica o tema visual personalizado do app (cores, tipografia)
                val navController = rememberNavController() // Cria o controlador que executa as mudanças de tela
                var tipoPerfil by remember { mutableStateOf("cliente") } //Armazena se o usuário é "Cliente ou Estabelecimento"
                var emailParaVerificar by remember { mutableStateOf("") } //Guarda temporariamente o email cadastrado
                var telaAtualBarra by remember { mutableStateOf("inicio") } // Guarda qual botão da barra inferior está selecionado
                var categoriaSelecionada by remember { mutableStateOf("") }

                val startDest = "inicio" // Define a tela que abrirá primeiro ao iniciar o app

                //Se a permissão foi negada, bloqueia o app e mostra a tela de permissão:
                if (!gpsPermitido && localizacaoAtual.contains("Permissão negada")) {
                    TelaBloqueioGPS(onTentarNovamente = { verificarPermissaoEObterGPS() })
                } else {
                    //Senão, mostra a estrutura normal com a barra inferior e Navegação
                    Scaffold(
                        bottomBar = { //Barra de botões na parte inferior da tela
                            if (tipoPerfil == "cliente") { // A Barra inferior só aparece para perfil Cliente
                                Surface(color = Color.White, shadowElevation = 8.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceAround // Distribui os botões com espaços
                                    ) {
                                        // Cada BotaoBarra navega para a rota correspondente quando clicado
                                        BotaoBarra("Início", Icons.Default.Home, telaAtualBarra == "inicio") {
                                            telaAtualBarra = "inicio"
                                            navController.navigate("inicio") { popUpTo("inicio") { inclusive = true } }
                                        }
                                        BotaoBarra("Explorar", Icons.Default.Search, telaAtualBarra == "explorar") {
                                            telaAtualBarra = "explorar"
                                            navController.navigate("explorar") { popUpTo("inicio") }
                                        }
                                        BotaoBarra("Favoritos", Icons.Default.Favorite, telaAtualBarra == "favoritos") {
                                            telaAtualBarra = "favoritos"
                                            navController.navigate("favoritos") { popUpTo("inicio") }
                                        }
                                        BotaoBarra("Agenda", Icons.Default.DateRange, telaAtualBarra == "agendamentos") {
                                            telaAtualBarra = "agendamentos"
                                            navController.navigate("agendamentos") { popUpTo("inicio") }
                                        }
                                        BotaoBarra("Perfil", Icons.Default.Person, telaAtualBarra == "perfil") {
                                            telaAtualBarra = "perfil"
                                            navController.navigate("perfil") { popUpTo("inicio") }
                                        }
                                    }
                                }
                            }
                        },
                        containerColor = Color(0xFFFFF9FA) // Cor de fundo para todas as telas
                    ) { innerPadding -> // O NavHost troca o conteúdo da tela dinamicamente sem recarregar a Activity
                        NavHost(
                            navController = navController,
                            startDestination = startDest,
                            modifier = Modifier.padding(innerPadding) // Evita que o conteúdo fique atrás da barra inferior
                        ) {
                            composable("inicio") {
                                TelaInicio(
                                    localizacaoAtual = localizacaoAtual,
                                    onExp = {
                                        categoriaSelecionada = ""
                                        telaAtualBarra = "explorar"
                                        navController.navigate("explorar")
                                    },
                                    onCategoriaClick = { categoria ->
                                        categoriaSelecionada = categoria
                                        telaAtualBarra = "explorar"
                                        navController.navigate("explorar")
                                    },
                                    onDet = { navController.navigate("detalhes") },
                                    onLoginClick = { navController.navigate("selecao_perfil") },
                                    isLogado = auth.currentUser != null
                                )
                            }
                            composable("explorar") { TelaExplorar(
                                categoriaSelecionada = categoriaSelecionada,
                                onLimparFiltro = { categoriaSelecionada = ""}
                            ) }
                            composable("favoritos") {
                                TelaFavoritos(
                                    isLogado = auth.currentUser != null,
                                    onLoginClick = { navController.navigate("selecao_perfil") }
                                )
                            }
                            composable("agendamentos") {
                                TelaAgendamentos(
                                    isLogado = auth.currentUser != null,
                                    onLoginClick = { navController.navigate("selecao_perfil") },
                                    onExplorar = { telaAtualBarra = "explorar"; navController.navigate("explorar") }
                                )
                            }
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
                                TelaPerfil(
                                    isLogado = auth.currentUser != null,
                                    onLoginClick = { navController.navigate("selecao_perfil") },
                                    onSair = {
                                        auth.signOut()
                                        telaAtualBarra = "inicio"
                                        navController.navigate("inicio") { popUpTo(0) }
                                    }
                                )
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
        //Checa se o app já tem a permissão para usar o GPS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gpsPermitido = true
            obterLocalizacaoGPS() //Se já tem permissão, calcula a localização
        } else {
            //se não tem, dispara o popup oficial do Android pedindo autorização
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun obterLocalizacaoGPS() {
        // Cria o cliente de localização do Google Play Services
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            //pede ao celular a ultima posição conhecida do GPS
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // O geocoder pega a latitude e longitude e converte em endereço humano
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val bairro = addresses[0].subLocality ?: addresses[0].thoroughfare ?: "Local atual"
                        val cidade = addresses[0].adminArea ?: ""
                        localizacaoAtual = "$bairro,$cidade" //Atualiza a variável observada pelo Compose
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
    //Se a aba estiver selecionada, pinta de roxo rosa, senão pinta de cinza
    val corTema = if (selecionado) Color(0xFF8E4772) else Color.Gray
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icone, contentDescription = titulo, tint = corTema, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(2.dp)) // espaço vertical entre ícone e texto
            Text(text = titulo, fontSize = 10.sp, color = corTema, fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

// --- TELA INICIAL (COM SUPORTE A VISITANTE / LOGADO) ---
@Composable
fun TelaInicio(
    localizacaoAtual: String,
    onExp: () -> Unit,
    onCategoriaClick: (String) -> Unit,
    onDet: () -> Unit,
    onLoginClick: () -> Unit,
    isLogado: Boolean
) {
    val categorias = listOf(
        Pair("Cabelo", Icons.Default.ContentCut),
        Pair("Unhas", Icons.Default.Brush),
        Pair("Barba", Icons.Default.Face),
        Pair("Estética", Icons.Default.AutoAwesome),
        Pair("Sobrancelha", Icons.Default.Visibility),
        Pair("Maquiagem", Icons.Default.Palette),
        Pair("Massagem", Icons.Default.Spa),
        Pair("Academia", Icons.Default.FitnessCenter)
    )

    val linhasDeCategorias = categorias.chunked(4)

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
            Text("Beleza Conectada!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Categorias", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E4772))
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                linhasDeCategorias.forEach { linha ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        linha.forEach { (nome, icone) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { onCategoriaClick(nome) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFFFF0F5), shape = CircleShape)
                                ) {
                                    Icon(
                                        imageVector = icone,
                                        contentDescription = nome,
                                        tint = Color(0xFF8E4772),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = nome,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2D2D2D)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var cnpj by remember { mutableStateOf("") }
    var cep by remember { mutableStateOf("") }
    var isCadastro by remember { mutableStateOf(false) } //Alterna entra aba Entrar e Cadastrar
    var mensagemErro by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) } //Exibe o indicador circular quando true
    var buscandoCnpj by remember { mutableStateOf(false) }
    var buscandoCep by remember { mutableStateOf(false) }

    // Efeito automático para consultar o CNPJ ao digitar 14 dígitos
    LaunchedEffect(cnpj) {
        val cnpjLimpo = cnpj.filter { it.isDigit() }
        if (cnpjLimpo.length == 14 && isCadastro && isEstabelecimento) {
            buscandoCnpj = true
            mensagemErro = ""
            try {
                val resposta = RetrofitClient.brasilApi.consultarCnpj(cnpjLimpo)
                buscandoCnpj = false
                val nomeEncontrado = resposta.nome_fantasia.takeIf { !it.isNullOrBlank() }
                    ?: resposta.razao_social ?: ""

                if (nomeEncontrado.isNotEmpty()) {
                    nome = nomeEncontrado
                }

                val situacao = resposta.descricao_situacao_cadastral ?: ""
                if (situacao.isNotEmpty() && !situacao.equals("ATIVA", ignoreCase = true)) {
                    mensagemErro = "Atenção: Situação do CNPJ na Receita Federal é $situacao."
                } else {
                    Toast.makeText(context, "CNPJ verificado! Nome autopreenchido.", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                buscandoCnpj = false
                mensagemErro = "CNPJ não encontrado na Receita Federal."
            }
        }
    }

    // Efeito automático para consultar o CEP ao digitar 8 dígitos
    LaunchedEffect(cep) {
        val cepLimpo = cep.filter { it.isDigit() }
        if (cepLimpo.length == 8 && isCadastro && isEstabelecimento) {
            buscandoCep = true
            mensagemErro = ""
            try {
                val respCep = RetrofitClient.brasilApi.consultarCep(cepLimpo)
                buscandoCep = false
                val rua = respCep.street ?: ""
                val bairro = respCep.neighborhood ?: ""
                val cidade = respCep.city ?: ""
                val estado = respCep.state ?: ""
                Toast.makeText(context, "Endereço: $rua, $bairro - $cidade/$estado", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                buscandoCep = false
                mensagemErro = "CEP não encontrado ou erro de conexão."
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
        } catch (_: Exception) {
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
            OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome completo / Razão Social") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            if (isEstabelecimento) {
                OutlinedTextField(
                    value = cnpj,
                    onValueChange = { cnpj = it },
                    label = { Text("CNPJ do Estabelecimento (14 dígitos)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (buscandoCnpj) {
                            CircularProgressIndicator(color = Color(0xFF8E4772), modifier = Modifier.size(20.dp))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cep,
                    onValueChange = { cep = it },
                    label = { Text("CEP do Endereço (8 dígitos)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (buscandoCep) {
                            CircularProgressIndicator(color = Color(0xFF8E4772), modifier = Modifier.size(20.dp))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)

        if (mensagemErro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mensagemErro, color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // validações em tempo de execução
                val emailValido = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                val temOitoDigitos = senha.length >= 8
                val temMaiuscula = senha.any { it.isUpperCase() }
                val temMinuscula = senha.any { it.isLowerCase() }
                val temEspecial = senha.any { !it.isLetterOrDigit() }

                if (!emailValido) {
                    mensagemErro = "Insira um e-mail válido."
                    return@Button
                }

                if (isCadastro) {
                    if (nome.isBlank()) {
                        mensagemErro = "Por favor, preencha o seu nome."
                        return@Button
                    }
                    if (isEstabelecimento && cnpj.length < 14) {
                        mensagemErro = "Por favor, insira um CNPJ válido."
                        return@Button
                    }
                    if (!temOitoDigitos || !temMaiuscula || !temMinuscula || !temEspecial) {
                        mensagemErro = "Senha fraca. Siga os requisitos solicitados."
                        return@Button
                    }
                }

                carregando = true
                mensagemErro = ""

                if (isCadastro) {
                    // Executa o cadastro no Firebase Auth
                    auth.createUserWithEmailAndPassword(email, senha)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid ?: ""
                                val db = FirebaseFirestore.getInstance()

                                val dadosUsuario = hashMapOf(
                                    "uid" to userId,
                                    "nome" to nome,
                                    "email" to email,
                                    "tipoPerfil" to if (isEstabelecimento) "estabelecimento" else "cliente",
                                    "cnpj" to if (isEstabelecimento) cnpj else "",
                                    "statusAprovacao" to if (isEstabelecimento) "PENDENTE" else "APROVADO",
                                    "criadoEm" to System.currentTimeMillis()
                                )

                                db.collection("usuarios").document(userId)
                                    .set(dadosUsuario)
                                    .addOnSuccessListener {
                                        carregando = false
                                        auth.currentUser?.sendEmailVerification()
                                        onAguardandoVerificacao(email)
                                    }
                                    .addOnFailureListener { e ->
                                        carregando = false
                                        mensagemErro = "Erro ao guardar dados no banco: ${e.localizedMessage}"
                                    }
                            } else {
                                carregando = false
                                mensagemErro = task.exception?.localizedMessage ?: "Erro ao criar conta."
                            }
                        }
                } else {
                    // Executa o login no Firebase Auth
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
fun TelaExplorar(
    categoriaSelecionada: String,
    onLimparFiltro: () -> Unit
) {
    var textoBusca by remember { mutableStateOf("") }
    var filtroPreco by remember { mutableStateOf("") }
    var filtroDistancia by remember { mutableStateOf("") }
    var mostrarModalFiltro by remember { mutableStateOf(false) }

    val listaEstabelecimentos = remember {
        listOf(
            Estabelecimento("1", "Studio Aura", "Salão de beleza", "Cabelo", "Cortes e coloração", "Populares", "4.9", "128", "1.2 km", "Aberto", "$$$", false, "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500"),
            Estabelecimento("2", "Esmalteria Bella", "Manicure e Pedicure", "Unhas", "Unhas em gel e fibra", "Promoção", "4.8", "95", "0.8 km", "Aberto", "$$", false, "https://images.unsplash.com/photo-1604654894610-df63bc536371?w=500"),
            Estabelecimento("3", "Barbearia Viking", "Barbearia Tradicional", "Barba", "Corte masculino e barba", "Destaque", "4.7", "210", "2.5 km", "Aberto", "$$", false, "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500"),
            Estabelecimento("4", "Clínica Glow", "Estética Facial", "Estética", "Limpeza de pele e botox", "Novo", "5.0", "42", "3.1 km", "Aberto", "$$$$", false, "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=500")
        )
    }

    val estabelecimentosFiltrados = remember(
        categoriaSelecionada,
        textoBusca,
        filtroPreco,
        filtroDistancia
    ) {
        listaEstabelecimentos.filter { est ->
            val bateCategoria = categoriaSelecionada.isEmpty() || est.categoria.equals(categoriaSelecionada, ignoreCase = true)
            val bateTexto = textoBusca.isEmpty() || est.nome.contains(textoBusca, ignoreCase = true) || est.subcategoria.contains(textoBusca, ignoreCase = true)
            val batePreco = filtroPreco.isEmpty() || est.preco == filtroPreco
            val distNum = est.distancia.replace(" km", "").toDoubleOrNull() ?: 0.0
            val distMax = filtroDistancia.toDoubleOrNull() ?: 999.0
            val bateDistancia = filtroDistancia.isEmpty() || distNum <= distMax

            bateCategoria && bateTexto && batePreco && bateDistancia
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(20.dp)
    ) {
        Text("Explorar Estabelecimentos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textoBusca,
                onValueChange = { textoBusca = it },
                label = { Text("Buscar serviços ou salões...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (textoBusca.isNotEmpty()) {
                        IconButton(onClick = { textoBusca = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = { mostrarModalFiltro = true },
                shape = RoundedCornerShape(14.dp),
                color = if (filtroPreco.isNotEmpty() || filtroDistancia.isNotEmpty() || categoriaSelecionada.isNotEmpty()) Color(0xFF8E4772) else Color(0xFFEEEEEE),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtros",
                        tint = if (filtroPreco.isNotEmpty() || filtroDistancia.isNotEmpty() || categoriaSelecionada.isNotEmpty()) Color.White else Color(0xFF8E4772)
                    )
                }
            }
        }

        if (categoriaSelecionada.isNotEmpty() || filtroPreco.isNotEmpty() || filtroDistancia.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (categoriaSelecionada.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF8E4772), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Categoria: $categoriaSelecionada", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onLimparFiltro, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                if (filtroPreco.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF8E4772), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Preço: $filtroPreco", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { filtroPreco = "" }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                if (filtroDistancia.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF8E4772), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Até $filtroDistancia km", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { filtroDistancia = "" }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (estabelecimentosFiltrados.isEmpty()) {
            Text("Nenhum resultado encontrado no momento.", color = Color.Gray, fontSize = 14.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(estabelecimentosFiltrados.size) { index ->
                    val est = estabelecimentosFiltrados[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            if (est.imagemUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = est.imagemUrl,
                                    contentDescription = est.nome,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = est.nome, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
                                    Text(text = est.preco, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E4772))
                                }
                                Text(text = "${est.subcategoria} • ${est.distancia}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${est.avaliacao} (${est.avaliacoesCount})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarModalFiltro) {
        AlertDialog(
            onDismissRequest = { mostrarModalFiltro = false },
            title = { Text("Filtrar Busca", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Faixa de Preço", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("$$", "$$$", "$$$$").forEach { preco ->
                            FilterChip(
                                selected = filtroPreco == preco,
                                onClick = { filtroPreco = if (filtroPreco == preco) "" else preco },
                                label = { Text(preco) }
                            )
                        }
                    }

                    Text("Distância Máxima", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1.0", "2.0", "5.0").forEach { dist ->
                            FilterChip(
                                selected = filtroDistancia == dist,
                                onClick = { filtroDistancia = if (filtroDistancia == dist) "" else dist },
                                label = { Text("Até $dist km") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarModalFiltro = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772))
                ) {
                    Text("Aplicar Filtros")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        filtroPreco = ""
                        filtroDistancia = ""
                        onLimparFiltro()
                        mostrarModalFiltro = false
                    }
                ) {
                    Text("Limpar Tudo", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun TelaFavoritos(
    isLogado: Boolean,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(Color(0xFFEEEEEE), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Meus Favoritos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(8.dp))
        if (!isLogado) {
            Text(
                text = "Você precisa estar conectado para visualizar e salvar seus estabelecimentos favoritos.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Entrar ou Cadastrar", color = Color.White)
            }
        } else {
            Text(
                text = "Você ainda não salvou nenhum estabelecimento como favorito.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TelaAgendamentos(
    isLogado: Boolean,
    onLoginClick: () -> Unit,
    onExplorar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(Color(0xFFEEEEEE), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Meus Agendamentos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(8.dp))
        if (!isLogado) {
            Text(
                text = "Você precisa estar conectado para visualizar seus agendamentos.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Entrar ou Cadastrar", color = Color.White)
            }
        } else {
            Text(
                text = "Você ainda não possui agendamentos realizados.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
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
fun TelaPerfil(
    isLogado: Boolean,
    onLoginClick: () -> Unit,
    onSair: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9FA)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(Color(0xFFEEEEEE), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Meu Perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2D2D))
        Spacer(modifier = Modifier.height(8.dp))
        if (!isLogado) {
            Text(
                text = "Você precisa estar conectado para acessar seu perfil e configurações.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E4772)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Entrar ou Cadastrar", color = Color.White)
            }
        } else {
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
