package com.devpro.pizzatime.feature.admin.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.devpro.pizzatime.R

enum class AdminMenuCategory(
    @get:StringRes val labelRes: Int,
) {
    SIGNATURE(R.string.manage_menu_category_signature),
    CLASSIC(R.string.manage_menu_category_classic),
    VEGGIE(R.string.manage_menu_category_veggie),
}

data class AdminMenuUiModel(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val category: AdminMenuCategory,
    @get:DrawableRes val imageRes: Int,
    val isAvailable: Boolean,
)