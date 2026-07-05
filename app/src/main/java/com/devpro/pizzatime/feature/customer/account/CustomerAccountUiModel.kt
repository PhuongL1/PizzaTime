package com.devpro.pizzatime.feature.customer.account

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.core.session.UserRole

data class CustomerAccountUiModel(
    val fullName: String,
    val tierName: String,
    val doughPoints: Int,
    val email: String,
    val phone: String,
    val deliveryAddress: String,
    val deliveryLat: Double? = null,
    val deliveryLng: Double? = null,
    val avatarUrl: String,
    @param:DrawableRes val avatarRes: Int,
    val lifetimeSpendText: String = "",
    val completedOrdersText: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val active: Boolean = true,
)
