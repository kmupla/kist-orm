plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    `maven-publish`
}

group = "io.knative.kistorm"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "maven-publish")
}