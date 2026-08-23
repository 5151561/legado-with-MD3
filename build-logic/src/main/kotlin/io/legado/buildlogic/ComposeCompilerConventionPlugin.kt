package io.legado.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Compose 编译器的统一配置，:app 与所有 compose 库模块共用。
 *
 * 稳定性配置必须共用：同一个类在不同模块被推断成不同的稳定性，
 * 跳过行为就会不一致，问题只在部分界面复现，很难查。
 *
 * 指标默认关闭（会拖慢编译），开启方式：
 *   ./gradlew :app:assembleAppRelease -Plegado.composeCompilerReports=true
 * 产物在各模块 build/compose-reports/ 下的 *-composables.txt / *-classes.txt。
 *
 * 写成普通 Plugin 而不是 build-logic 里其它约定用的 precompiled script：
 * 同样内容放进 legado.compose.compiler.gradle.kts 时，编译产物里只剩 plugins{} 块，
 * 脚本主体被静默丢弃（构建照样成功，配置静默失效）。
 */
class ComposeCompilerConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<ComposeCompilerGradlePluginExtension> {
            stabilityConfigurationFiles.add(
                rootProject.layout.projectDirectory.file("config/compose-stability.conf")
            )

            val reportsEnabled = providers.gradleProperty("legado.composeCompilerReports")
                .map(String::toBoolean)
                .getOrElse(false)
            if (reportsEnabled) {
                metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
                reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
            }
        }
    }
}
