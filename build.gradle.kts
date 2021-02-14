import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    application
}

group = "com.vitekkor"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}


dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    api("com.github.kotlinx.ast:grammar-kotlin-parser-antlr-kotlin:f9c929a8ab")
    implementation("com.github.kotlinx.ast:grammar-kotlin-parser-antlr-kotlin:f9c929a8ab")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClassName = "MainKt"
}