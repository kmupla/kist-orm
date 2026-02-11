plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    `maven-publish`
}

group = "io.github.kmupla.kistorm"
version = "0.8.0"

subprojects {
    group = rootProject.group
    version = rootProject.version

    if (name != "kist-sample-test") {
        apply(plugin = "maven-publish")

        publishing {
            repositories {
                maven {
                    name = "BuildRepo"
                    url = uri("${rootProject.buildDir}/repo")
                }
            }
        }
    }
}