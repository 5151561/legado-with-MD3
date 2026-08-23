import com.android.build.api.dsl.LibraryExtension
import io.legado.buildlogic.ComposeCompilerConventionPlugin

plugins {
    id("legado.android.library")
}

// 同项目内用 gradlePlugin{} 注册的插件无法从 precompiled script 的 plugins{} 块解析，
// 只能按类型 apply。:app 走正常构建脚本，仍用 id("legado.compose.compiler")。
apply<ComposeCompilerConventionPlugin>()

extensions.configure<LibraryExtension> {
    buildFeatures {
        compose = true
    }
}
