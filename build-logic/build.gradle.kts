plugins {
    `kotlin-dsl`
}

group = "io.legado.buildlogic"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation(
        "org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}"
    )
    // feature ui 模块声明自己的 @Serializable 路由，序列化插件因此是 UI 约定的一部分。
    implementation(
        "org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}"
    )
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        register("composeCompiler") {
            id = "legado.compose.compiler"
            implementationClass = "io.legado.buildlogic.ComposeCompilerConventionPlugin"
        }
    }
}
