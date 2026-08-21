plugins {
    id("legado.feature.api")
}

android {
    namespace = "io.legado.app.feature.bookshelf.api"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
