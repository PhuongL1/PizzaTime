package com.devpro.pizzatime.feature.customer.customize

import androidx.annotation.DrawableRes

data class BuildPizzaUiModel(
    val productName: String,
    @param:DrawableRes val previewImageRes: Int,
    val selectedCrust: String,
    val sizes: List<PizzaSizeOption>,
    val sauces: List<SauceOption>,
    val cheeses: List<CheeseOption>,
    val toppingGroups: List<ToppingGroup>,
)

data class PizzaSizeOption(
    val id: String,
    val label: String,
    val price: Double,
    val selected: Boolean = false,
)

data class SauceOption(
    val id: String,
    val name: String,
    val subtitle: String,
    @param:DrawableRes val previewBackgroundRes: Int,
    val selected: Boolean = false,
)

data class CheeseOption(
    val id: String,
    val name: String,
    val extraPrice: Double,
    val included: Boolean = false,
    val selected: Boolean = false,
)

data class ToppingGroup(
    val title: String,
    val items: List<String>,
)