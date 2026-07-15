package com.devpro.pizzatime.feature.staff.dashboard

data class StaffOrderUiModel(
    val orderId: String,
    val displayOrderCode: String = orderId,
    val customerName: String,
    val timeAgo: String,
    val fulfillmentType: StaffFulfillmentType,
    val orderSummary: String,
    val price: String,
    val status: StaffOrderStatus,
    val canConfirmOrder: Boolean = true,
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
