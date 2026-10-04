import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val versionProps =
    Properties().apply {
        rootProject.file("version.properties").inputStream().use { load(it) }
    }
val appVersionName: String = versionProps.getProperty("versionName")
val appVersionCode: Int =
    appVersionName.split(".").let { (major, minor, patch) ->
        major.toInt() * 1_000_000 + minor.toInt() * 1_000 + patch.toInt()
    }

kotlin {
    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

android {
    namespace = "io.github.behnooddev.voidmanager.android"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "io.github.behnooddev.voidmanager"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildTypes {
        release {
            // Shrinking is switched on in the polish phase, once release builds can be tested on devices.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // Bouncy Castle ships classes for JNDI and other packages that Android does not have. They are never
        // loaded here, because only the lightweight API (Argon2id, HKDF) is used and no provider is registered.
        disable += "InvalidPackage"
    }
}

dependencies {
    implementation(project(":app:shared"))
    implementation(project(":core:crypto"))
    implementation(project(":core:security"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
    implementation(libs.kotlinx.coroutines.core)
}
