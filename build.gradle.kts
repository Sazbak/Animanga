plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val logbackVersion: String by project

dependencies {
    testImplementation(kotlin("test"))
    dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") }
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation(project(":jikan-client"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.example.MainKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "org.example.MainKt" // Ensure the entry point is set
    }
}