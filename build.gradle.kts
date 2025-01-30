plugins {
    kotlin("jvm") version "2.1.0"
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