package com.devpro.pizzatime.feature.kitchen.detail

import androidx.annotation.DrawableRes

data class KitchenOrderDetailUiModel(
    val orderId: String,
    val receivedAgo: String,
    val status: KitchenOrderDetailStatus,
    val item: KitchenOrderDetailItemUiModel,
    val allergyTitle: String?,
    val allergyMessage: String?,
    val customerRequest: String,
    val tags: List<String>,
)

data class KitchenOrderDetailItemUiModel(
    val name: String,
    val size: String,
    val crust: String,
    val toppings: List<String>,
    @param:DrawableRes val imageRes: Int,
)

enum class KitchenOrderDetailStatus {
    PENDING,
    PREPARING,
    BAKING,
    READY,
    CANCELLED,
}