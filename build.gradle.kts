plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "com.filedownloader"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}

