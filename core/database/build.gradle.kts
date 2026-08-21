plugins {
    id("legado.android.library")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.legado.app.core.database"
}

dependencies {
    api(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.core.ktx)
    implementation(libs.gson)
    implementation(libs.flexbox)
    implementation(libs.json.path)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
}

room {
    schemaDirectory("$rootDir/app/schemas")
}

ksp {
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
    arg("room.generateKotlin", "false")
}
