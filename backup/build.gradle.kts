import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Cofre de backup: Kotlin puro. Cifra e confere a integridade de uma cópia dos dados.
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
    testImplementation(libs.junit)
}
