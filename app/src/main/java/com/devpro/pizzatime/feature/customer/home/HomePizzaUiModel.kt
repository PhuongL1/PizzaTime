package com.devpro.pizzatime.feature.customer.home

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.R

data class BestSellerPizzaUiModel(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val rating: String = "",
    @param:DrawableRes val imageRes: Int,
    val imageUrl: String = "",
    val isFavorite: Boolean = false,
)

data class ChefPizzaUiModel(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val label: String,
    val rating: String,
    @param:DrawableRes val imageRes: Int,
    val imageUrl: String = "",
)

object FakeHomeData {

    val bestSellers = listOf(
        BestSellerPizzaUiModel(
            id = "diavola_noir",
            name = "Diavola Noir",
            description = "Spicy salami, honey, activated charcoal dough.",
            price = "$24.00",
            imageRes = R.drawable.img_welcome_hero,
            isFavorite = true,
        ),
        BestSellerPizzaUiModel(
            id = "copper_garden",
            name = "Copper Garden",
            description = "Artichoke hearts, smoked provolone, burnt sage.",
            price = "$22.00",
            imageRes = R.drawable.img_welcome_hero,
            isFavorite = false,
        ),
    )

    val chefSelections = listOf(
        ChefPizzaUiModel(
            id = "amalfi_coast",
            name = "The Amalfi Coast",
            description = "Lemon-zest ricotta, prosciutto di parma, arugula.",
            price = "$28.00",
            label = "AWARD WINNING",
            rating = "★ 4.9",
            imageRes = R.drawable.img_welcome_hero,
        ),
    )
}
