plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    `maven-publish`
}

group = "io.github.kmupla.kistorm"
version = "0.8.0"

subprojects {
    apply(plugin = "maven-publish")
}