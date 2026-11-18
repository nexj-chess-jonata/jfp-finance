import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Motor financeiro: Kotlin puro. Não depende de Android, de interface nem de IA.
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
    api(project(":core"))
    testImplementation(libs.junit)
}
