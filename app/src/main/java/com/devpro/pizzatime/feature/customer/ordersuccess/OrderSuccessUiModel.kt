package com.devpro.pizzatime.feature.customer.ordersuccess

import androidx.annotation.DrawableRes

data class OrderSuccessUiModel(
    val orderId: String,
    val title: String,
    val message: String,
    val estimatedArrival: String,
    val statusLabel: String,
    @param:DrawableRes val heroImageRes: Int,
)