plugins {
    id("legado.android.compose")
}

android {
    namespace = "io.legado.app.core.navigation"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.navigation3.runtime)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
