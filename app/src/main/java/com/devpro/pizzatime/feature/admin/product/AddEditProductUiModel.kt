package com.devpro.pizzatime.feature.admin.product

import androidx.annotation.DrawableRes

data class AddEditProductUiModel(
    val productId: String,
    val isEditMode: Boolean,
    val isAvailable: Boolean,
    val name: String,
    val description: String,
    val category: String,
    val imageUrl: String,
    val basePrice: Double,
    val sizes: List<ProductOptionUiModel>,
    val crustOptions: List<ProductOptionUiModel>,
    val toppings: List<String>,
    @param:DrawableRes val heroImageRes: Int,
)

data class ProductOptionUiModel(
    val label: String,
    val selected: Boolean,
)
