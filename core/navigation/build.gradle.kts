plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.legado.app.core.navigation"
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
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    lint {
        checkDependencies = true
        targetSdk = 37
    }

    testOptions {
        targetSdk = 37
    }
}

dependencies {
    api(libs.androidx.navigation3.runtime)
}
