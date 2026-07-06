package com.devpro.pizzatime.feature.shipper.dashboard

enum class ShipperDeliveryStatus {
    ACTIVE,
    ASSIGNED,
    DELIVERED,
}

data class ShipperDeliveryUiModel(
    val orderId: String,
    val displayOrderCode: String = orderId,
    val customerName: String,
    val address: String,
    val etaLabel: String,
    val paymentLabel: String,
    val paymentAmount: String,
    val status: ShipperDeliveryStatus,
    val shipperId: String = "",
    val rawStatus: String = "",
)

data class ShipperDashboardUiModel(
    val activeOrders: List<ShipperDeliveryUiModel>,
    val deliveredOrders: List<ShipperDeliveryUiModel>,
    val activeOrderCount: Int,
    val readyOrderCount: Int,
    val completedOrderCount: Int,
    val deliveryEarnings: Double,
)
