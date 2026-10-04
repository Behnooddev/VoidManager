import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":app:shared"))
    implementation(project(":core:crypto"))
    implementation(project(":core:security"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "io.github.behnooddev.voidmanager.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = "VoidManager"
            // The macOS installer requires a major version above 0, so the installer version is
            // decoupled from version.properties until the application reaches 1.0.0.
            packageVersion = "1.0.0"
        }
    }
}
