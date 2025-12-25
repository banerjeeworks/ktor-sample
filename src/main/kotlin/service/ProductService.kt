package com.example.service

import com.example.model.Discount
import com.example.model.Product
import io.ktor.http.HttpStatusCode
import java.sql.Connection
import java.sql.SQLException

class ProductService(private val getConnection: () -> Connection) {

  fun listByCountry(country: String): List<Product> {
    val sql = """
      SELECT p.id, p.name, p.base_price, p.country,
             d.discount_id, d.percent
      FROM products p
      LEFT JOIN product_discounts d ON d.product_id = p.id
      WHERE LOWER(p.country) = LOWER(?)
      ORDER BY p.id
    """.trimIndent()

    getConnection().use { connection ->
      connection.prepareStatement(sql).use { ps ->
        ps.setString(1, country)
        ps.executeQuery().use { rs ->
          val map = linkedMapOf<String, Product>()
          while (rs.next()) {
            val id = rs.getString("id")
          val product = map.getOrPut(id) {
            Product(
              id = id,
              name = rs.getString("name"),
              basePrice = rs.getDouble("base_price"),
              country = rs.getString("country"),
              discounts = mutableListOf()
            )
          }
          val discountId = rs.getString("discount_id")
          if (discountId != null) {
            val percent = rs.getDouble("percent")
            (product.discounts as MutableList).add(Discount(discountId, percent))
          }
        }
        return map.values.toList()
      }
      }
    }
  }

  data class ApplyResult(val applied: Boolean, val status: HttpStatusCode)

  fun applyDiscount(productId: String, discountId: String, percent: Double): ApplyResult {
    require(percent > 0.0 && percent < 100.0) { "percent must be > 0 and < 100" }

    // Ensure product exists
    getConnection().use { connection ->
      connection.prepareStatement("SELECT 1 FROM products WHERE id = ?").use { ps ->
        ps.setString(1, productId)
        ps.executeQuery().use { rs ->
          if (!rs.next()) {
            return ApplyResult(applied = false, status = HttpStatusCode.NotFound)
          }
        }
      }

    // Try to insert the discount. Unique PK(product_id, discount_id) enforces idempotency.
    try {
      connection.prepareStatement(
        "INSERT INTO product_discounts(product_id, discount_id, percent) VALUES(?, ?, ?)"
      ).use { ps ->
        ps.setString(1, productId)
        ps.setString(2, discountId)
        ps.setDouble(3, percent)
        ps.executeUpdate()
      }
      return ApplyResult(applied = true, status = HttpStatusCode.OK)
    } catch (ex: SQLException) {
      // Duplicate key -> already applied -> idempotent success
      val message = ex.message?.lowercase() ?: ""
      val isDuplicate =
        message.contains("unique") || message.contains("duplicate") || message.contains("primary key")
      return if (isDuplicate) ApplyResult(applied = false, status = HttpStatusCode.OK)
      else throw ex
    }
    }
  }
}

data class ApplyDiscountRequest(val discountId: String, val percent: Double)
