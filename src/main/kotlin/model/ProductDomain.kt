package com.example.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.round

@Serializable
data class Discount(
  val discountId: String,
  val percent: Double,
)

@Serializable
data class Product(
  val id: String,
  val name: String,
  val basePrice: Double,
  val country: String,
  val discounts: List<Discount> = emptyList(),
) {
  val finalPrice: Double
    get() {
      val vat = VatRules.vatFor(country)
      val discountFactor = max(0.0, 1.0 - discounts.sumOf { it.percent } / 100.0)
      val vatFactor = 1.0 + vat / 100.0
      return (basePrice * discountFactor * vatFactor).let { round(it * 100.0) / 100.0 }
    }
}

object VatRules {
  fun vatFor(country: String): Double = when (country.lowercase()) {
    "sweden" -> 25.0
    "germany" -> 19.0
    "france" -> 20.0
    else -> 0.0
  }
}
