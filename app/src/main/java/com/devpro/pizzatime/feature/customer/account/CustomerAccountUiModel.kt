package com.devpro.pizzatime.feature.customer.account

import androidx.annotation.DrawableRes

data class CustomerAccountUiModel(
    val fullName: String,
    val tierName: String,
    val doughPoints: Int,
    val email: String,
    val phone: String,
    val deliveryAddress: String,
    val avatarUrl: String,
    @param:DrawableRes val avatarRes: Int,
)
