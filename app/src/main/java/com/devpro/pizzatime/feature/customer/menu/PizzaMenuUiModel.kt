package com.devpro.pizzatime.feature.customer.menu

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.R

data class PizzaMenuUiModel(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val rating: String,
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,
    @param:DrawableRes val imageRes: Int,
    val imageUrl: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val sizeOptions: List<String> = emptyList(),
    val crustOptions: List<String> = emptyList(),
    val toppingOptions: List<String> = emptyList(),
)

object FakePizzaMenuData {

    val pizzas = listOf(
        PizzaMenuUiModel(
            id = "truffle_noir",
            name = "Truffle Noir",
            description = "Black truffle cream, wild mushrooms, fresh buffalo mozzarella, and aged...",
            price = "\$24.00",
            rating = "4.9",
            imageRes = R.drawable.img_welcome_hero,
        ),
        PizzaMenuUiModel(
            id = "rustic_margherita",
            name = "Rustic Margherita",
            description = "San Marzano tomatoes, fior di latte, extra virgin olive oil, and organic basil.",
            price = "\$19.00",
            rating = "4.7",
            imageRes = R.drawable.img_welcome_hero,
        ),
        PizzaMenuUiModel(
            id = "midnight_copper",
            name = "Midnight Copper",
            description = "Spicy salami, hot honey drizzle, gorgonzola dolce, and caramelized...",
            price = "\$22.00",
            rating = "4.8",
            imageRes = R.drawable.img_welcome_hero,
        ),
        PizzaMenuUiModel(
            id = "verdant_hearth",
            name = "Verdant Hearth",
            description = "Roasted seasonal greens, pine nuts, garlic confit, and herb-infused oil.",
            price = "\$21.00",
            rating = "4.6",
            imageRes = R.drawable.img_welcome_hero,
        ),
    )
}
