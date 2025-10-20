pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
    }

    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "KistORM"
include("kist-orm")
include("kist-api")
include("kist-ksp")
include("kist-sample-test")