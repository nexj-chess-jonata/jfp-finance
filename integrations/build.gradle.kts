import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Importação de extratos: Kotlin puro. Só monta propostas; quem grava é o motor.
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
    api(project(":finance-engine"))
    testImplementation(libs.junit)
}
