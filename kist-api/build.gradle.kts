plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ksp)
    id("dev.mokkery") version "3.0.0"
}

kotlin {
    jvm()

    applyDefaultHierarchyTemplate()

    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("co.touchlab:kermit:2.0.8")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeMain by getting {
            dependencies {
                implementation("co.touchlab:sqliter-driver:1.3.3")
            }
        }
        val nativeTest by getting

        val jvmMain by getting {
            dependencies {
                implementation("org.xerial:sqlite-jdbc:3.51.1.0")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}