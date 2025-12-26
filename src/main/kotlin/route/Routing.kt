package com.example.route

import com.example.service.ApplyDiscountRequest
import com.example.database.ProductServiceKey
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
  routing {
    get("/") { call.respondText("OK") }

    get("/products") {
      val country = call.request.queryParameters["country"]
      if (country.isNullOrBlank()) {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "country is required"))
        return@get
      }
      val svc = this@configureRouting.attributes[ProductServiceKey]
      val products = svc.listByCountry(country)
      call.respond(HttpStatusCode.OK, products)
    }

    put("/products/{id}/discount") {
      val id = call.parameters["id"]
      if (id.isNullOrBlank()) {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
        return@put
      }
      val req = call.receive<ApplyDiscountRequest>()
      val svc = this@configureRouting.attributes[ProductServiceKey]
      val result = try {
        svc.applyDiscount(id, req.discountId, req.percent)
      } catch (e: IllegalArgumentException) {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        return@put
      }
      call.respond(result.status, mapOf("applied" to result.applied))
    }
  }
}
