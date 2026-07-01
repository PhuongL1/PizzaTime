package com.devpro.pizzatime.feature.admin.product

import com.devpro.pizzatime.R

object FakeAddEditProductData {

    fun getProduct(productId: String?): AddEditProductUiModel {
        return AddEditProductUiModel(
            productId = productId.orEmpty().ifBlank { DEFAULT_PRODUCT_ID },
            isEditMode = productId != null,
            isAvailable = true,
            name = "Midnight Truffle & Pecorino",
            description = "Shaved black summer truffles, aged pecorino romano, and cold-pressed extra virgin olive oil on our 48-hour fermented sourdough base.",
            category = "Signature Artisan",
            basePrice = 28.00,
            sizes = listOf(
                ProductOptionUiModel("Small (10\")", true),
                ProductOptionUiModel("Medium (12\")", false),
                ProductOptionUiModel("Large (14\")", true),
            ),
            crustOptions = listOf(
                ProductOptionUiModel("48h Sourdough", true),
                ProductOptionUiModel("Gluten-Free Artisan", false),
                ProductOptionUiModel("Charred Neapolitan", true),
            ),
            toppings = listOf(
                "Fresh Basil",
                "Buffalo Mozzarella",
                "Black Truffle Oil",
                "Wild Mushrooms",
                "Prosciutto di Parma",
            ),
            heroImageRes = R.drawable.img_pizza_time,
        )
    }

    private const val DEFAULT_PRODUCT_ID = "pizza_midnight_truffle"
}