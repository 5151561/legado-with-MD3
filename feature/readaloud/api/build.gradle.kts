plugins { alias(libs.plugins.android.library) }
android {
    namespace = "io.legado.app.feature.readaloud.api"
    compileSdk = 37
    defaultConfig { minSdk = 26; consumerProguardFiles("consumer-rules.pro") }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
    kotlin.jvmToolchain(21)
    lint { checkDependencies = true; targetSdk = 37 }
}
dependencies { api(libs.kotlinx.coroutines.core); testImplementation(libs.junit) }
