package com.devpro.pizzatime.feature.admin.orders

enum class AdminOrderStatus {
    ALL,
    PENDING,
    CONFIRMED,
    READY,
    SHIPPED,
    DELIVERED,
    CANCELLED,
}

data class AdminOrderUiModel(
    val orderId: String,
    val displayOrderCode: String = orderId,
    val customerName: String,
    val phone: String,
    val itemsSummary: String,
    val total: Double,
    val status: AdminOrderStatus,
    val metaText: String,
)
