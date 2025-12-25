val h2Version: String by project
val koinVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val postgresVersion: String by project

plugins {
  kotlin("jvm") version "2.2.21"
  id("io.ktor.plugin") version "3.3.2"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

group = "com.example"
version = "0.0.1"

application {
  mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
  implementation("io.ktor:ktor-server-default-headers")
  implementation("io.ktor:ktor-server-di")
  implementation("io.insert-koin:koin-ktor:$koinVersion")
  implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
  implementation("io.ktor:ktor-server-core")
  implementation("io.ktor:ktor-server-content-negotiation")
  implementation("io.ktor:ktor-serialization-kotlinx-json")
  implementation("org.postgresql:postgresql:$postgresVersion")
  implementation("com.h2database:h2:$h2Version")
  implementation("io.ktor:ktor-serialization-jackson")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.ktor:ktor-server-call-logging")
  implementation("io.ktor:ktor-server-call-id")
  implementation("io.ktor:ktor-server-netty")
  implementation("ch.qos.logback:logback-classic:$logbackVersion")
  implementation("io.ktor:ktor-server-config-yaml")
  testImplementation("io.ktor:ktor-server-test-host")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}
