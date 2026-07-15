package com.devpro.pizzatime.feature.customer.account

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.shared.location.DeliveryCoordinate

data class CustomerAccountUiModel(
    val fullName: String,
    val tierName: String,
    val doughPoints: Int,
    val email: String,
    val phone: String,
    val deliveryAddress: String,
    val deliveryCoordinate: DeliveryCoordinate? = null,
    val avatarUrl: String,
    @param:DrawableRes val avatarRes: Int,
    val lifetimeSpendText: String = "",
    val completedOrdersText: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val active: Boolean = true,
)
