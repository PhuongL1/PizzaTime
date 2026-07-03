package com.devpro.pizzatime.feature.customer.orderdetail

data class CustomerOrderStatusHistoryUiModel(
    val status: String,
    val actorRole: String,
    val note: String,
    val timeText: String,
)
