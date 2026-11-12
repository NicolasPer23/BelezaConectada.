plugins {
    alias(libs.plugins.android.application) //configura o projeto como uma aplicação android
    alias(libs.plugins.kotlin.compose) // habilita o suporte do compilador kotlin para jetpack compose
    id("com.google.gms.google-services") version "4.5.0" // Plugin do google para conectar o app aos serviços do firebase
}

android {
    namespace = "com.example.belezaconectada"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.belezaconectada"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    buildToolsVersion = "36.0.0"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom)) //BOM Bill of Materials garante compatibilidade de versões entre pacotes Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.compose.material:material-icons-extended-android:1.7.8") // Ícones extras do Material
    // Jetpack Compose & Material 3
    implementation("androidx.compose.ui:ui:1.12.1")
    implementation("androidx.compose.material3:material3:1.4.0") //Biblioteca do Material Design
    implementation("androidx.navigation:navigation-compose:2.10.2") // Permite Navegar entre telas no compose utilizando rotas
    implementation("com.google.android.gms:play-services-location:21.4.0") //Biblioteca do Google para obter a posição GPS do Dispositivo
    implementation(platform("com.google.firebase:firebase-bom:33.9.0")) // Gerenciador de versões do serviços do Firebase
    implementation("com.google.firebase:firebase-analytics") // Análise de uso da aplicação
    implementation("com.google.firebase:firebase-auth") // Autentificação de utilizadores (Email/senha/google)
    implementation("com.google.firebase:firebase-firestore") //Banco de Dados NoSQL em tempo real do Firebase (Armazena perfis, agendamentos e estabelecimentos)
    implementation("com.google.android.gms:play-services-auth:21.3.0") //Integração Nativa para login com a conta Google
    // Retrofit & Gson para consultas de API na internet (BrasilAPI CNPJ/CEP)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // Coil para carregamento de imagens da internet no Compose
    implementation("io.coil-kt:coil-compose:2.7.0")
}