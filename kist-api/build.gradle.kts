plugins {
    kotlin("multiplatform")
//    kotlin("plugin.serialization")
}

group = "io.rss.knative.tools.kistorm"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()

    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
//                implementation(project(":webviewkt"))
//                implementation(libs.kotlinxSerializationJson)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeMain by getting {
            dependsOn(commonMain)

            dependencies {
                implementation("co.touchlab:sqliter-driver:1.3.3")
//                implementation("app.cash.sqldelight:native-driver:2.1.0")

                when {
                    hostOs == "Mac OS X" && isArm64 -> implementation("co.touchlab:sqliter-driver-macosarm64:1.3.3")
                    hostOs == "Mac OS X" && !isArm64 -> implementation("co.touchlab:sqliter-driver-macosx64:1.3.3")
                    hostOs == "Linux" && isArm64 -> implementation("co.touchlab:sqliter-driver-linuxarm64:1.3.3")
                    hostOs == "Linux" && !isArm64 -> implementation("co.touchlab:sqliter-driver-linuxx64:1.3.3")
                    isMingwX64 -> implementation("co.touchlab:sqliter-driver-mingwx64:1.3.3")
                }
            }
        }
        /*val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }*/
    }
}