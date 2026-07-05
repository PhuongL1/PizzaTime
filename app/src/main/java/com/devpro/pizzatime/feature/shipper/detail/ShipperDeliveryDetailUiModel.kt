package com.devpro.pizzatime.feature.shipper.detail

data class ShipperDeliveryDetailUiModel(
    val orderId: String,
    val displayOrderCode: String = orderId,
    val customerName: String,
    val address: String,
    val courierNote: String,
    val paymentAmount: String,
    val paymentMethod: String,
    val paymentStatus: String = "",
    val items: List<ShipperPaymentItemUiModel>,
    val storeName: String = "",
    val pickupAddress: String = "",
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val storePhone: String = "",
    val customerPhone: String = "",
    val deliveryLat: Double? = null,
    val deliveryLng: Double? = null,
    val distanceKm: Double? = null,
    val deliveryFee: String = "",
)

data class ShipperPaymentItemUiModel(
    val name: String,
    val price: String,
)
