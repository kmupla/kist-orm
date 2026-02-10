plugins {
    alias(libs.plugins.ksp)
    kotlin("jvm")
    `maven-publish`
    signing
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name.set("Kist KSP")
                description.set("KSP Processor for Kist ORM")
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
                    url.set("https://github.com/kmupla/kist-orm")
                }
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

dependencies {
    implementation(libs.kspSymbolProcessingApi)
    implementation(project(":kist-api"))
    implementation(kotlin("stdlib"))
//    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
//    implementation(libs.kotlinxSerializationJson)

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("org.mockito:mockito-core:5.12.0")
}

tasks.test {
    useJUnitPlatform()
}