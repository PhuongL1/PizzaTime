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
    @param:DrawableRes val imageRes: Int?,
    val cardType: CustomerFavoriteCardType,
)

data class CustomerFavoritePairingUiModel(
    val title: String,
    val subtitle: String,
)

enum class CustomerFavoriteCardType {
    FEATURED,
    COMPACT,
}