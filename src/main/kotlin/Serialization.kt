package com.example

import com.fasterxml.jackson.databind.*
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
  install(ContentNegotiation) {
    // Support both Jackson and Kotlinx JSON
    jackson {
      enable(SerializationFeature.INDENT_OUTPUT)
    }
    json()
  }
}
