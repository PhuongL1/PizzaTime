package com.devpro.pizzatime.feature.staff.dashboard

data class StaffOrderUiModel(
    val orderId: String,
    val customerName: String,
    val timeAgo: String,
    val fulfillmentType: StaffFulfillmentType,
    val orderSummary: String,
    val price: String,
    val status: StaffOrderStatus,
)

enum class StaffFulfillmentType {
    DELIVERY,
    COLLECTION,
}

enum class StaffOrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    CANCELLED,
}
