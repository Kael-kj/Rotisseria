
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.resources)
    implementation(libs.logback.classic)
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.0.0")
    implementation("org.mongodb:bson-kotlinx:5.0.0")
    implementation("org.jmdns:jmdns:3.5.8")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
