import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Módulo Kotlin puro: sem Android. Modelos de domínio, dinheiro e contratos.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // api: os contratos do core expõem Flow para os outros módulos.
    api(libs.coroutines.core)
    testImplementation(libs.junit)
}
