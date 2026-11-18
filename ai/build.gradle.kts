import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Assistente: Kotlin puro. Lê os resultados do motor financeiro e nunca altera dados.
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
