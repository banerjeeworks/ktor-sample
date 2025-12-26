package com.example.database

import com.example.service.ProductService
import io.ktor.server.application.*
import java.sql.Connection
import java.sql.DriverManager
import io.ktor.util.AttributeKey

val ProductServiceKey = AttributeKey<ProductService>("ProductService")

fun Application.configureDatabases() {
  // Keep a single connection for the test assessment. Otherwise, use a Hikari Connection pool.
  val embedded = environment.config.propertyOrNull("db.embedded")?.getString()?.toBooleanStrictOrNull() ?: true
  val dbConnection: Connection = connectToPostgres(embedded)
  environment.monitor.subscribe(ApplicationStopping) {
    try { dbConnection.close() } catch (_: Exception) {
      log.error("Failed to close database connection")
    }
  }
  // Initialize schema and seed data for products
  ProductSchema.init(dbConnection, log)
  val svc = ProductService { connectToPostgres(embedded) }
  attributes.put(ProductServiceKey, svc)
}

/**
 * Makes a connection to a Postgres database.
 *
 * To connect to your running Postgres process,
 * please specify the following parameters in your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 *
 * @param embedded -- if [true] defaults to an embedded database for tests that runs locally in the same process.
 * In this case you don't have to provide any parameters in the config file, and you don't have to run a process.
 * */
fun Application.connectToPostgres(embedded: Boolean): Connection {
  Class.forName("org.postgresql.Driver")
  return if (embedded) {
    log.info("Using embedded H2 database for testing; replace this flag to use postgres")
    DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
  } else {
    val url = environment.config.property("postgres.url").getString()
    log.info("Connecting to postgres database at $url")
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()
    DriverManager.getConnection(url, user, password)
  }
}
