plugins { id("legado.feature.api") }
android {
    namespace = "io.legado.app.feature.ai.api"
}
dependencies { api(libs.kotlinx.coroutines.core); testImplementation(libs.junit) }
