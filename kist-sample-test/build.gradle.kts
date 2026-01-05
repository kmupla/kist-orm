plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ksp)
}

group = "io.knative.samples"
version = "1.0-SNAPSHOT"

val hostOs = System.getProperty("os.name")
val isArm64 = System.getProperty("os.arch") == "aarch64"
val isMingwX64 = hostOs.startsWith("Windows")

kotlin {

    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
//            executable {
//                entryPoint = "app.main"
//                linkerOpts.add("-lsqlite3")
//                debuggable = true
//                optimized = false
//            }

            executable {
                entryPoint = "app.main"
                if (isMingwX64) {
                    linkerOpts.add("-L${project.file("lib-win").absolutePath}")
                }
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

        val nativeMain by getting {
            dependsOn(commonMain)
            kotlin.srcDir("build/generated/ksp/native/nativeMain/kotlin")
        }
    }
}

tasks.named("compileKotlinNative") {
    dependsOn("kspKotlinNative")
}

if (isMingwX64) {
    tasks.register<Copy>("copySqliteDll") {
        from(project.file("lib-win/sqlite3.dll"))
        into(layout.buildDirectory.dir("bin/native/debugExecutable"))
    }

    tasks.named("runDebugExecutableNative") {
        dependsOn("copySqliteDll")
    }
}

dependencies {
    add("kspNative", project(":kist-ksp"))
    kspCommonMainMetadata(project(":kist-ksp"))
}