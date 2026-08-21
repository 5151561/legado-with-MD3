plugins {
    id("legado.feature.impl")
}

android {
    namespace = "io.legado.app.feature.bookshelf.impl"
}

dependencies {
    api(project(":feature:bookshelf:api"))
    implementation(project(":core:database"))

    implementation(libs.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
