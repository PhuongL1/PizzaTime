package com.devpro.pizzatime.feature.customer.memberqr

import androidx.annotation.DrawableRes

data class CustomerMemberQrUiModel(
    val tierLabel: String,
    val memberTitle: String,
    val pointsLabel: String,
    val currentPoints: Int,
    val targetPoints: Int,
    val memberSinceLabel: String,
    val memberSinceValue: String,
    val qrInstruction: String,
    @param:DrawableRes val qrImageRes: Int,
)