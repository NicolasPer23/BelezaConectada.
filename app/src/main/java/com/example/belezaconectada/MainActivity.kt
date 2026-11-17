package com.example.belezaconectada

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.belezaconectada.ui.components.BotaoBarra
import com.example.belezaconectada.ui.screens.*
import com.example.belezaconectada.ui.theme.BelezaConectadaTheme
import com.example.belezaconectada.ui.viewmodel.AuthViewModel
import com.example.belezaconectada.ui.viewmodel.ExplorarViewModel
import com.google.android.gms.location.LocationServices
import java.util.Locale

class MainActivity : ComponentActivity() {

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
        enableEdgeToEdge()
        verificarPermissaoEObterGPS()

        setContent {
            BelezaConectadaTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val explorarViewModel: ExplorarViewModel = viewModel()

                var tipoPerfil by remember { mutableStateOf("cliente") }
                var emailParaVerificar by remember { mutableStateOf("") }
                var telaAtualBarra by remember { mutableStateOf("inicio") }

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
                                    onExp = {
                                        explorarViewModel.updateCategoriaSelecionada("")
                                        telaAtualBarra = "explorar"
                                        navController.navigate("explorar")
                                    },
                                    onCategoriaClick = { categoria ->
                                        explorarViewModel.updateCategoriaSelecionada(categoria)
                                        telaAtualBarra = "explorar"
                                        navController.navigate("explorar")
                                    },
                                    onDet = { navController.navigate("detalhes") },
                                    onLoginClick = { navController.navigate("selecao_perfil") },
                                    isLogado = authViewModel.isUserLoggedIn()
                                )
                            }
                            composable("explorar") {
                                TelaExplorar(
                                    viewModel = explorarViewModel,
                                    categoriaSelecionadaParam = explorarViewModel.categoriaSelecionada,
                                    onLimparFiltro = { explorarViewModel.updateCategoriaSelecionada("") }
                                )
                            }
                            composable("favoritos") {
                                TelaFavoritos(
                                    isLogado = authViewModel.isUserLoggedIn(),
                                    onLoginClick = { navController.navigate("selecao_perfil") }
                                )
                            }
                            composable("agendamentos") {
                                TelaAgendamentos(
                                    isLogado = authViewModel.isUserLoggedIn(),
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
                                    viewModel = authViewModel,
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
                                    viewModel = authViewModel,
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
                                    viewModel = authViewModel,
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
                                    isLogado = authViewModel.isUserLoggedIn(),
                                    onLoginClick = { navController.navigate("selecao_perfil") },
                                    onSair = {
                                        authViewModel.signOut()
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
                                    if (!authViewModel.isUserLoggedIn()) {
                                        navController.navigate("selecao_perfil")
                                    } else {
                                        telaAtualBarra = "agendamentos"
                                        navController.navigate("agendamentos") { popUpTo("inicio") }
                                    }
                                }
                            }
                            composable("painel_estabelecimento") {
                                TelaPainelEstabelecimento {
                                    tipoPerfil = "cliente"
                                    telaAtualBarra = "inicio"
                                    navController.navigate("inicio") { popUpTo("painel_estabelecimento") { inclusive = true } }
                                }
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
        } catch (_: SecurityException) {
            localizacaoAtual = "São Paulo, SP"
        }
    }
}
