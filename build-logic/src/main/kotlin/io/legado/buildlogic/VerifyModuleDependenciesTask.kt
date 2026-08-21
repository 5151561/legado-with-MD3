package io.legado.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class VerifyModuleDependenciesTask : DefaultTask() {

    @get:Input
    abstract val violations: ListProperty<String>

    @TaskAction
    fun verify() {
        check(violations.get().isEmpty()) {
            violations.get().joinToString(prefix = "模块依赖治理失败:\n", separator = "\n")
        }
    }
}
