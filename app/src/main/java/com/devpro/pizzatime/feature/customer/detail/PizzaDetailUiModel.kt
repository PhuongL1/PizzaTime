package com.devpro.pizzatime.feature.customer.detail

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.R

data class PizzaDetailUiModel(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val rating: String,
    val time: String,
    val kcal: String,
    @param:DrawableRes val imageRes: Int,
    val toppings: List<ExtraToppingUiModel>,
    val imageUrl: String = "",
    val sizeOptions: List<String> = emptyList(),
    val crustOptions: List<String> = emptyList(),
)

data class ExtraToppingUiModel(
    val id: String,
    val name: String,
    val price: String,
    val isSelected: Boolean = false,
)

object FakePizzaDetailData {

    val truffleNoir = PizzaDetailUiModel(
        id = "truffle_noir",
        name = "Truffle Noir",
        description = "Hand-stretched charcoal dough, black truffle cream, wild mushrooms, and 24-month aged parmesan. A sophisticated urban masterpiece.",
        price = "$24.00",
        rating = "4.9",
        time = "25-30 MIN",
        kcal = "840 KCAL",
        imageRes = R.drawable.img_welcome_hero,
        toppings = listOf(
            ExtraToppingUiModel(
                id = "extra_cheese",
                name = "Extra Cheese",
                price = "+$2.00",
            ),
            ExtraToppingUiModel(
                id = "black_olives",
                name = "Black Olives",
                price = "+$1.50",
            ),
            ExtraToppingUiModel(
                id = "red_onions",
                name = "Red Onions",
                price = "+$1.00",
            ),
            ExtraToppingUiModel(
                id = "truffle_oil",
                name = "Truffle Oil",
                price = "+$3.00",
            ),
        ),
    )
}
