plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ksp)
    id("dev.mokkery") version "3.0.0"
    `maven-publish`
    signing
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

// Create an empty Javadoc JAR to satisfy Sonatype requirements for non-JVM targets
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }

    publications.withType<MavenPublication> {
        // Add the empty javadoc jar to all targets so Sonatype doesn't reject it
        artifact(javadocJar)

        groupId = project.group.toString()
        artifactId = when (name) {
            "kotlinMultiplatform" -> project.name
            else -> "${project.name}-$name"
        }
        version = project.version.toString()

        pom {
            name.set("Kist API")
            description.set("ORM for Kotlin Multiplatform")
            url.set("https://github.com/kmupla/kist-orm")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("ssricardo")
                    name.set("Ricardo SS")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/kmupla/kist-orm.git")
                developerConnection.set("scm:git:ssh://github.com/kmupla/kist-orm.git")
                url.set("https://github.com/kmupla/kist-orm")
            }
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")

    if (!signingKey.isNullOrEmpty()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}


tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}