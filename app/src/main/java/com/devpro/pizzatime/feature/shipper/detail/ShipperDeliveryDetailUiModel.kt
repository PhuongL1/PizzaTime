package com.devpro.pizzatime.feature.shipper.detail

data class ShipperDeliveryDetailUiModel(
    val orderId: String,
    val customerName: String,
    val address: String,
    val courierNote: String,
    val paymentAmount: String,
    val paymentMethod: String,
    val items: List<ShipperPaymentItemUiModel>,
    val storeName: String = "",
    val pickupAddress: String = "",
    val storePhone: String = "",
    val customerPhone: String = "",
)

data class ShipperPaymentItemUiModel(
    val name: String,
    val price: String,
)
