package com.devpro.pizzatime.feature.customer.favorites

import com.devpro.pizzatime.R

object FakeCustomerFavoritesData {

    fun getFavorites(): CustomerFavoritesUiModel {
        return CustomerFavoritesUiModel(
            title = "Your Cravings",
            subtitle = "Hand-picked artisan selections for the midnight epicurean.",
            favorites = listOf(
                CustomerFavoriteItemUiModel(
                    id = "midnight_truffle",
                    name = "The Midnight Truffle",
                    description = "Black truffle cream, wild mushrooms, thyme, and fontina cheese.",
                    price = 24.0,
                    badge = "CHEF'S PICK",
                    imageRes = R.drawable.img_pizza_time,
                    cardType = CustomerFavoriteCardType.FEATURED,
                ),
                CustomerFavoriteItemUiModel(
                    id = "spicy_honey_diavola",
                    name = "Spicy Honey Diavola",
                    description = "Calabrian chili, local honey, spicy salami, and aged pecorino.",
                    price = 22.0,
                    badge = null,
                    imageRes = R.drawable.img_pizza_time,
                    cardType = CustomerFavoriteCardType.FEATURED,
                ),
                CustomerFavoriteItemUiModel(
                    id = "artisan_margherita",
                    name = "Artisan Margherita",
                    description = "Fresh basil, mozzarella, and slow-cooked tomato sauce.",
                    price = 18.0,
                    badge = null,
                    imageRes = R.drawable.img_pizza_time,
                    cardType = CustomerFavoriteCardType.COMPACT,
                ),
            ),
            pairing = CustomerFavoritePairingUiModel(
                title = "Weekly Pairings",
                subtitle = "Recommended wines for your favorites",
            ),
        )
    }
}