package com.devpro.pizzatime.feature.shipper.detail

import com.devpro.pizzatime.feature.order.DeliveryHandoffStatus
import com.devpro.pizzatime.feature.order.PaymentMethod
import com.devpro.pizzatime.feature.order.PaymentStatus
import com.devpro.pizzatime.shared.location.DeliveryCoordinate

data class ShipperDeliveryDetailUiModel(
    val orderId: String,
    val displayOrderCode: String = orderId,
    val customerName: String,
    val address: String,
    val courierNote: String,
    val paymentAmount: String,
    val paymentMethod: String,
    val paymentStatus: String = "",
    val paymentMethodValue: PaymentMethod = PaymentMethod.COD,
    val paymentStatusValue: PaymentStatus = PaymentStatus.NOT_REQUIRED,
    val deliveryHandoffStatusValue: DeliveryHandoffStatus = DeliveryHandoffStatus.NOT_REQUIRED,
    val items: List<ShipperPaymentItemUiModel>,
    val storeName: String = "",
    val pickupAddress: String = "",
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val storePhone: String = "",
    val customerPhone: String = "",
    val deliveryCoordinate: DeliveryCoordinate? = null,
    val deliveryFee: String = "",
    val navigationAddress: String = "",
    val orderStatus: String = "",
    val shipperId: String = "",
)

data class ShipperPaymentItemUiModel(
    val name: String,
    val price: String,
)
