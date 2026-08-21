plugins {
    id("legado.android.library")
}

android {
    namespace = "io.legado.app.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
}
