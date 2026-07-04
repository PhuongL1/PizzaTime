package com.devpro.pizzatime.feature.admin.menu

import com.devpro.pizzatime.R

object FakeAdminMenuData {

    private val items = mutableListOf(
        AdminMenuUiModel(
            id = "pizza_001",
            name = "Spiced Honey Heat",
            description = "Sourdough base, spicy calabrese, wildflower honey, and fresh basil.",
            price = "$24.00",
            basePrice = 24.00,
            categoryId = "SIGNATURE",
            category = AdminMenuCategory.SIGNATURE,
            imageUrl = "",
            imageRes = R.drawable.img_welcome_hero,
            isAvailable = true,
        ),
        AdminMenuUiModel(
            id = "pizza_002",
            name = "The Black Truffle",
            description = "Wild porcini, fontina cheese, shaved black truffles, and aromatic herbs.",
            price = "$32.00",
            basePrice = 32.00,
            categoryId = "SIGNATURE",
            category = AdminMenuCategory.SIGNATURE,
            imageUrl = "",
            imageRes = R.drawable.img_welcome_hero,
            isAvailable = true,
        ),
        AdminMenuUiModel(
            id = "pizza_003",
            name = "Prosciutto Night",
            description = "Cured ham, aged balsamic, parmigiano reggiano, and baby arugula.",
            price = "$26.00",
            basePrice = 26.00,
            categoryId = "SIGNATURE",
            category = AdminMenuCategory.SIGNATURE,
            imageUrl = "",
            imageRes = R.drawable.img_welcome_hero,
            isAvailable = false,
        ),
        AdminMenuUiModel(
            id = "pizza_004",
            name = "Midnight Margherita",
            description = "San Marzano tomato, mozzarella, basil oil, and sea salt.",
            price = "$18.50",
            basePrice = 18.50,
            categoryId = "CLASSIC",
            category = AdminMenuCategory.CLASSIC,
            imageUrl = "",
            imageRes = R.drawable.img_welcome_hero,
            isAvailable = true,
        ),
        AdminMenuUiModel(
            id = "pizza_005",
            name = "Tuscan Garden",
            description = "Roasted vegetables, pesto cream, cherry tomato, and fresh herbs.",
            price = "$21.00",
            basePrice = 21.00,
            categoryId = "VEGGIE",
            category = AdminMenuCategory.VEGGIE,
            imageUrl = "",
            imageRes = R.drawable.img_welcome_hero,
            isAvailable = true,
        ),
    )

    fun getItems(): List<AdminMenuUiModel> {
        return items.toList()
    }

    fun toggleAvailability(itemId: String) {
        val index = items.indexOfFirst { item -> item.id == itemId }
        if (index == -1) return

        val currentItem = items[index]
        items[index] = currentItem.copy(
            isAvailable = !currentItem.isAvailable,
        )
    }
}
