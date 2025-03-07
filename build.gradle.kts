plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
    id("org.openapi.generator") version "7.11.0"
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$rootDir/openapi/jikan-api-docs.json")
    packageName.set("org.openapitools.client")
    configOptions.set(
        mapOf("enumPropertyNaming" to "UPPERCASE")

    )
}

sourceSets {
    main {
        kotlin.srcDirs(
            project.layout.buildDirectory.dir("generate-resources/main/src/main/kotlin").get()
        )
    }
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
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("com.github.ajalt.clikt:clikt-markdown:5.0.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.moshi:moshi-adapters:1.15.1")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
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