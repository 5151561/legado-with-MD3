plugins { alias(libs.plugins.android.library); alias(libs.plugins.compose.compiler) }
android {
    namespace = "io.legado.app.feature.readaloud.ui"
    compileSdk = 37
    defaultConfig { minSdk = 26; consumerProguardFiles("consumer-rules.pro") }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
    kotlin.jvmToolchain(21)
    lint { checkDependencies = true; targetSdk = 37 }
}
dependencies {
    api(project(":feature:readaloud:api")); implementation(project(":core:designsystem"))
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.activity.compose)
    implementation(libs.androidx.compose.foundation); implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.ui)
    implementation(libs.compose.materialIcons)
    implementation(libs.androidx.lifecycle.runtime.compose); implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.koin.bom)); implementation(libs.koin.core); implementation(libs.koin.compose); implementation(libs.koin.compose.viewmodel)
    testImplementation(libs.junit); testImplementation(libs.kotlinx.coroutines.test)
}
