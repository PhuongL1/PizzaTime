package com.devpro.pizzatime.feature.customer.cart

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.R

data class CartItemUiModel(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    @param:DrawableRes val imageRes: Int,
)

object FakeCartData {

    val items = listOf(
        CartItemUiModel(
            id = "truffle_noir",
            name = "Truffle Noir",
            price = 28.0,
            quantity = 1,
            imageRes = R.drawable.img_welcome_hero,
        ),
        CartItemUiModel(
            id = "rustic_margherita",
            name = "Rustic Margherita",
            price = 20.0,
            quantity = 1,
            imageRes = R.drawable.img_welcome_hero,
        ),
    )

    const val deliveryFee = 2.0
    const val discount = 5.0
}