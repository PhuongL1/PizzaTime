package com.devpro.pizzatime.feature.shipper.dashboard

enum class ShipperDeliveryStatus {
    ACTIVE,
    ASSIGNED,
}

data class ShipperDeliveryUiModel(
    val orderId: String,
    val customerName: String,
    val address: String,
    val etaLabel: String,
    val paymentLabel: String,
    val paymentAmount: String,
    val status: ShipperDeliveryStatus,
    val shipperId: String = "",
)
