package com.devpro.pizzatime.feature.customer.checkout

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.R

data class CheckoutOrderItemUiModel(
    val id: String,
    val name: String,
    val optionText: String,
    val price: Double,
    @param:DrawableRes val imageRes: Int,
)

object FakeCheckoutData {

    val orderItems = listOf(
        CheckoutOrderItemUiModel(
            id = "margherita_verace",
            name = "Margherita Verace",
            optionText = "x1 · Large · Extra Basil",
            price = 24.0,
            imageRes = R.drawable.img_welcome_hero,
        ),
        CheckoutOrderItemUiModel(
            id = "diavola_nocturnal",
            name = "Diavola Nocturnal",
            optionText = "x1 · Medium",
            price = 28.0,
            imageRes = R.drawable.img_welcome_hero,
        ),
    )

    const val deliveryFee = 4.5
}