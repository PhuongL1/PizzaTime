package com.devpro.pizzatime.feature.customer.cart

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.R

data class CartItemUiModel(
    val id: String,
    val name: String,
    val description: String = "",
    val price: Double,
    val quantity: Int,
    @param:DrawableRes val imageRes: Int,
    val selectedSize: String = "",
    val selectedCrust: String = "",
    val selectedToppings: List<String> = emptyList(),
    val imageUrl: String = "",
) {
    val cartKey: String
        get() = listOf(id, selectedSize, selectedCrust, selectedToppings.joinToString("|"))
            .joinToString("::")
}

object FakeCartData {

    val items = listOf(
        CartItemUiModel(
            id = "truffle_noir",
            name = "Truffle Noir",
            description = "Black truffle cream, wild mushrooms, and aged parmesan.",
            price = 28.0,
            quantity = 1,
            imageRes = R.drawable.img_welcome_hero,
        ),
        CartItemUiModel(
            id = "rustic_margherita",
            name = "Rustic Margherita",
            description = "San Marzano tomato, mozzarella, and basil oil.",
            price = 20.0,
            quantity = 1,
            imageRes = R.drawable.img_welcome_hero,
        ),
    )

    const val deliveryFee = 2.0
    const val discount = 5.0
}
