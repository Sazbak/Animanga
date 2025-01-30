plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "animanga"
include(":jikan-client")
project(":jikan-client").projectDir = file("jikan-client")


