package com.example

import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*


fun Application.configureHTTP() {
  install(DefaultHeaders) {
    header("X-Engine", "Ktor")
  }
}
