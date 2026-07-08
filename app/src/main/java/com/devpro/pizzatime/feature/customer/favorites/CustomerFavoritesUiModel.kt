package com.devpro.pizzatime.feature.customer.favorites

import androidx.annotation.DrawableRes

data class CustomerFavoritesUiModel(
    val title: String,
    val subtitle: String,
    val favorites: List<CustomerFavoriteItemUiModel>,
    val pairing: CustomerFavoritePairingUiModel,
)

data class CustomerFavoriteItemUiModel(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val badge: String?,
    val categoryId: String = "",
    val categoryName: String = "",
    @param:DrawableRes val imageRes: Int?,
    val cardType: CustomerFavoriteCardType,
    val imageUrl: String = "",
    val sizeOptions: List<String> = emptyList(),
    val crustOptions: List<String> = emptyList(),
    val toppingOptions: List<String> = emptyList(),
)

data class CustomerFavoritePairingUiModel(
    val title: String,
    val subtitle: String,
)

enum class CustomerFavoriteCardType {
    FEATURED,
    COMPACT,
}
