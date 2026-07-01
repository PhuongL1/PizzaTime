package com.devpro.pizzatime.feature.customer.customize

import com.devpro.pizzatime.R

object FakeBuildPizzaData {

    fun getBuildPizza(): BuildPizzaUiModel {
        return BuildPizzaUiModel(
            productName = "Premium Artisan Pizza",
            previewImageRes = R.drawable.img_pizza_time,
            selectedCrust = "Classic Neapolitan (Charred)",
            sizes = listOf(
                PizzaSizeOption(
                    id = "size_12",
                    label = "12\"",
                    price = 18.0,
                ),
                PizzaSizeOption(
                    id = "size_14",
                    label = "14\"",
                    price = 22.0,
                    selected = true,
                ),
                PizzaSizeOption(
                    id = "size_16",
                    label = "16\"",
                    price = 26.0,
                ),
            ),
            sauces = listOf(
                SauceOption(
                    id = "san_marzano",
                    name = "San Marzano",
                    subtitle = "CLASSIC RED",
                    previewBackgroundRes = R.drawable.bg_build_pizza_sauce_red,
                    selected = true,
                ),
                SauceOption(
                    id = "bianca_garlic",
                    name = "Bianca Garlic",
                    subtitle = "RICH WHITE",
                    previewBackgroundRes = R.drawable.bg_build_pizza_sauce_white,
                ),
                SauceOption(
                    id = "basil_pesto",
                    name = "Basil Pesto",
                    subtitle = "AROMATIC GREEN",
                    previewBackgroundRes = R.drawable.bg_build_pizza_sauce_green,
                ),
            ),
            cheeses = listOf(
                CheeseOption(
                    id = "buffalo_mozzarella",
                    name = "Buffalo Mozzarella",
                    extraPrice = 0.0,
                    included = true,
                    selected = true,
                ),
                CheeseOption(
                    id = "creamy_burrata",
                    name = "Creamy Burrata",
                    extraPrice = 4.0,
                ),
                CheeseOption(
                    id = "aged_gorgonzola",
                    name = "Aged Gorgonzola",
                    extraPrice = 2.5,
                ),
            ),
            toppingGroups = listOf(
                ToppingGroup(
                    title = "MEATS",
                    items = listOf(
                        "Prosciutto di Parma",
                        "Spicy Soppressata",
                        "Wild Boar Salami",
                    ),
                ),
                ToppingGroup(
                    title = "VEGGIES",
                    items = listOf(
                        "Truffled Mushrooms",
                        "Kalamata Olives",
                        "Roasted Artichoke",
                    ),
                ),
            ),
        )
    }
}