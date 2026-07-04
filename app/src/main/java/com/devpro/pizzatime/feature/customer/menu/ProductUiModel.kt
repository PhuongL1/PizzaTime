package com.devpro.pizzatime.feature.customer.menu

data class ProductUiModel(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: Double,
    val imageUrl: String,
    val rating: Double,
    val available: Boolean,
    val categoryId: String = "",
    val categoryName: String = "",
    val sizeOptions: List<String> = emptyList(),
    val crustOptions: List<String> = emptyList(),
    val toppingOptions: List<String> = emptyList(),
)

