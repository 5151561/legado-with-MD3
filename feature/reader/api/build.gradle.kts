plugins {
    id("legado.feature.api")
}

android {
    namespace = "io.legado.app.feature.reader.api"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
