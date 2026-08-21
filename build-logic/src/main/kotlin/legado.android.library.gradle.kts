import com.android.build.api.dsl.LibraryExtension
import io.legado.buildlogic.VerifyModuleDependenciesTask
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.ProjectDependency

plugins {
    id("com.android.library")
}

extensions.configure<LibraryExtension> {
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    lint {
        checkDependencies = true
        targetSdk = 37
    }

    testOptions {
        targetSdk = 37
    }
}

val verifyModuleDependencies = tasks.register<VerifyModuleDependenciesTask>(
    "verifyModuleDependencies"
) {
    group = "verification"
    description = "校验 core/feature 模块依赖方向与正式 impl 最小交付物"
    violations.convention(emptyList())
}

afterEvaluate {
    verifyModuleDependencies.configure {
        val modulePath = project.path
        val projectTargets = configurations
            .flatMap { configuration -> configuration.dependencies }
            .filterIsInstance<ProjectDependency>()
            .map { dependency -> dependency.path }
            .toSet()
        val detectedViolations = buildList {
            projectTargets.forEach { target ->
                when {
                    modulePath.startsWith(":feature:") && modulePath.endsWith(":ui") &&
                        (target == ":app" || target == ":core:database" ||
                            (target.startsWith(":feature:") && target.endsWith(":impl"))) ->
                        add("$modulePath 禁止依赖 $target")

                    modulePath.startsWith(":feature:") && modulePath.endsWith(":api") &&
                        (target == ":app" || target.startsWith(":core:") ||
                            (target.startsWith(":feature:") && !target.endsWith(":api"))) ->
                        add("$modulePath 禁止依赖 $target")

                    modulePath.startsWith(":feature:") && modulePath.endsWith(":impl") &&
                        target == ":app" -> add("$modulePath 禁止依赖 :app")

                    modulePath.startsWith(":core:") &&
                        (target == ":app" || target.startsWith(":feature:")) ->
                        add("$modulePath 禁止依赖 $target")
                }
            }

            if (modulePath.startsWith(":feature:") && modulePath.endsWith(":impl")) {
                val apiPath = modulePath.removeSuffix(":impl") + ":api"
                if (apiPath !in projectTargets) {
                    add("$modulePath 必须依赖对应 API 模块 $apiPath")
                }
                val contractTests = fileTree("src/test") {
                    include("**/*ContractTest.kt")
                }.files
                if (contractTests.isEmpty()) {
                    add("$modulePath 必须提供可复用的 API *ContractTest.kt")
                }
                val mainText = fileTree("src/main") {
                    include("**/*.kt")
                }.files.joinToString("\n") { it.readText() }
                if ("org.koin.dsl.module" !in mainText || "module {" !in mainText) {
                    add("$modulePath 必须声明唯一的 Koin module")
                }
            }
        }

        violations.set(detectedViolations)
    }
}

tasks.configureEach {
    if (name != verifyModuleDependencies.name &&
        (name.startsWith("assemble") || name.startsWith("compile") || name.startsWith("test"))
    ) {
        dependsOn(verifyModuleDependencies)
    }
}
