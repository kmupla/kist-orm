plugins {
    alias(libs.plugins.ksp)
    kotlin("jvm")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
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