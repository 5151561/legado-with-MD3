plugins { id("legado.feature.ui") }
android {
    namespace = "io.legado.app.feature.rss.ui"
}
dependencies {
    api(project(":feature:rss:api")); implementation(project(":core:designsystem"))
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.activity.compose)
    implementation(libs.androidx.compose.foundation); implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.ui)
    implementation(libs.compose.materialIcons)
    implementation(libs.androidx.lifecycle.runtime.compose); implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose); implementation(libs.kotlinx.collections.immutable); implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.koin.bom)); implementation(libs.koin.core); implementation(libs.koin.compose); implementation(libs.koin.compose.viewmodel)
    testImplementation(libs.junit); testImplementation(libs.kotlinx.coroutines.test)
}
