plugins { id("legado.feature.api") }
android {
    namespace = "io.legado.app.feature.readaloud.api"
}
dependencies { api(libs.kotlinx.coroutines.core); testImplementation(libs.junit) }
