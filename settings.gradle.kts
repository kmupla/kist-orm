pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
    }
}

dependencyResolutionManagement {
}

rootProject.name = "KistORM"
include("kist-api")
include("kist-ksp")
include("kist-sample-test")