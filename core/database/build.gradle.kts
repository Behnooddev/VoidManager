import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "io.github.behnooddev.voidmanager.core.database"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.sqldelight.runtime)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.sqlcipher.android)
            implementation(libs.androidx.sqlite)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.jvm.driver)
            // Replaces the stock SQLite JDBC driver: same package, adds SQLCipher-compatible encryption.
            implementation(libs.willena.sqlite.jdbc)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

sqldelight {
    databases {
        create("VoidManagerDatabase") {
            packageName.set("io.github.behnooddev.voidmanager.core.database")
            dialect(libs.sqldelight.dialect)
        }
    }
}
