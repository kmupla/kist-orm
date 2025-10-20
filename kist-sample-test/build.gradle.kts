plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ksp)
//    kotlin("plugin.serialization")
//    id("io.rss.knative.tools.resource-generator")
}

group = "io.rss.knative.samples"
version = "1.0-SNAPSHOT"

kotlin {

    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("apple")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("apple")
        hostOs == "Linux" && isArm64 -> linuxArm64("linux")
        hostOs == "Linux" && !isArm64 -> linuxX64("linux")
        isMingwX64 -> mingwX64("windows")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "app.main"
                linkerOpts.add("-lsqlite3")
                debuggable = true
                optimized = false
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kist-api"))
                implementation(libs.kotlinxSerializationJson)
            }
        }

        val appleMain by getting {
            dependsOn(commonMain)
        }
    }
}

tasks.named("compileKotlinApple") {
    dependsOn("kspKotlinApple")
}

dependencies {
    add("kspApple", project(":kist-ksp"))
    kspCommonMainMetadata(project(":kist-ksp"))
}