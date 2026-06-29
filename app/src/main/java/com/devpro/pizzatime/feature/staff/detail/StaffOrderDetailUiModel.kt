package com.devpro.pizzatime.feature.staff.detail

import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderStatus

data class StaffOrderDetailUiModel(
    val orderId: String,
    val receivedAgo: String,
    val status: StaffOrderStatus,
    val itemName: String,
    val size: String,
    val crust: String,
    val toppings: List<String>,
    val imageRes: Int,
    val allergyTitle: String?,
    val allergyMessage: String?,
    val customerRequest: String?,
    val tags: List<String>,
)

