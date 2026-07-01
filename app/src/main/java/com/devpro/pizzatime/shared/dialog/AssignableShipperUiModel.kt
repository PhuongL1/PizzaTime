package com.devpro.pizzatime.shared.dialog

import androidx.annotation.DrawableRes

data class AssignableShipperUiModel(
    val id: String,
    val name: String,
    val activeDeliveryCount: Int,
    val etaMinutes: Int,
    val isAvailable: Boolean,
    @DrawableRes val avatarRes: Int,
)