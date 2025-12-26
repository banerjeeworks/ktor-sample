package com.example.database

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection

object ProductSchema {
  private const val CREATE_PRODUCTS =
    """
    CREATE TABLE IF NOT EXISTS products (
      id VARCHAR(64) PRIMARY KEY,
      name VARCHAR(255) NOT NULL,
      base_price DOUBLE PRECISION NOT NULL,
      country VARCHAR(64) NOT NULL
    );
    """

  private const val CREATE_PRODUCT_DISCOUNTS =
    """
    CREATE TABLE IF NOT EXISTS product_discounts (
      product_id VARCHAR(64) NOT NULL,
      discount_id VARCHAR(128) NOT NULL,
      percent DOUBLE PRECISION NOT NULL,
      PRIMARY KEY (product_id, discount_id),
      CONSTRAINT fk_pd_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
    );
    """

  fun init(connection: Connection, log: Logger) {
    connection.createStatement().use { st ->
      st.executeUpdate(CREATE_PRODUCTS)
      st.executeUpdate(CREATE_PRODUCT_DISCOUNTS)
    }

    // Seed a few products if the table is empty
    val count = connection.createStatement().use { st ->
      st.executeQuery("SELECT COUNT(*) FROM products").use { rs ->
        if (rs.next()) rs.getInt(1) else 0
      }
    }
    if (count == 0) {
      log.info("Seeding initial products")
      seedProducts(connection)
    }
  }

  private fun seedProducts(connection: Connection) {
    val items = listOf(
      Triple("swe-001", "Vacuum Cleaner", Triple(199.99, "Sweden", 0.0)),
      Triple("swe-002", "Air Purifier", Triple(149.50, "Sweden", 0.0)),
      Triple("deu-001", "Coffee Maker", Triple(89.90, "Germany", 0.0)),
      Triple("fra-001", "Blender", Triple(59.00, "France", 0.0)),
    )
    connection.prepareStatement("INSERT INTO products(id, name, base_price, country) VALUES (?, ?, ?, ?)").use { ps ->
      for ((id, name, pack) in items) {
        val (price, country, _) = pack
        try {
          ps.setString(1, id)
          ps.setString(2, name)
          ps.setDouble(3, price)
          ps.setString(4, country)
          ps.addBatch()
        } catch (e: Exception) {
          LoggerFactory.getLogger("Slf4jExample::class.java").error("Failed to seed product: $id, $name, $price, $country", e)
        }
      }
      ps.executeBatch()
    }
  }
}