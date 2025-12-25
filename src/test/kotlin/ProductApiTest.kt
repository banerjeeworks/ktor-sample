package com.example

import com.example.model.Product
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProductApiTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  @DisplayName("Get products by country")
  fun getProductsByCountry_returnsSeeded() = testApplication {
    application { module() }

    val res = client.get("/products?country=Sweden")
    assertEquals(HttpStatusCode.OK, res.status)
    val body = res.bodyAsText()
    val products = json.decodeFromString(ListSerializer(Product.serializer()), body)
    assertTrue(products.isNotEmpty())
    assertTrue(products.any { it.country.equals("Sweden", ignoreCase = true) })
    assertTrue(body.contains("\"finalPrice\""))
  }

  @Test
  @DisplayName("Apply discount is idempotent under concurrency")
  fun applyDiscount_isIdempotent_underConcurrency() = testApplication {
    application { module() }

    val productId = "swe-001"
    val payload = "{" +
      "\"discountId\":\"DISC-TEST-1\"," +
      "\"percent\":10.0" +
      "}"

    // Fire many concurrent identical requests
    coroutineScope {
      (1..50).map {
        async {
          client.put("/products/$productId/discount") {
            contentType(ContentType.Application.Json)
            setBody(payload)
          }
        }
      }.awaitAll()
    }

    // Verify only one discount is applied
    val res = client.get("/products?country=Sweden")
    assertEquals(HttpStatusCode.OK, res.status)
    val products = json.decodeFromString(ListSerializer(Product.serializer()), res.bodyAsText())
    val product = products.firstOrNull { it.id == productId }
    assertNotNull(product)
    val discCount = product.discounts.count { it.discountId == "DISC-TEST-1" }
    assertEquals(1, discCount)


    val expected = ((product.basePrice * (1 - 0.10)) * 1.25)

    assertEquals(
      (kotlin.math.round(expected * 100.0) / 100.0),
      product.finalPrice,
      "Final price should reflect exactly one discount"
    )
  }
}
