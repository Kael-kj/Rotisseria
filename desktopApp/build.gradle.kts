import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.uiToolingPreview)
    implementation(compose.materialIconsExtended)

    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation(compose.desktop.currentOs)
    implementation(compose.desktop.linux_x64)
    implementation(compose.desktop.windows_x64)
    implementation("org.jmdns:jmdns:3.5.8")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.poi:poi:5.2.3")
}

compose.desktop {
    application {
        mainClass = "com.kenji.rotisseriaadmin.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.kenji.rotisseriaadmin"
            packageVersion = "1.0.0"
        }
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
    }
}