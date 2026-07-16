package com.devpro.pizzatime.feature.order

enum class PaymentMethod {
    COD,
    DEMO,
    VNPAY,
    UNKNOWN,
}

fun PaymentMethod.isPrepaid(): Boolean {
    return this == PaymentMethod.DEMO || this == PaymentMethod.VNPAY
}

enum class PaymentStatus {
    NOT_REQUIRED,
    PENDING,
    PAID,
    FAILED,
    EXPIRED,
    REFUNDED,
    UNKNOWN,
}

enum class DeliveryHandoffStatus {
    NOT_REQUIRED,
    LOCKED,
    AWAITING_CUSTOMER,
    CUSTOMER_CONFIRMED,
    COMPLETED,
    UNKNOWN,
}

data class OrderPaymentHandoffSnapshot(
    val orderStatus: String,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val deliveryHandoffStatus: DeliveryHandoffStatus,
    val customerId: String = "",
    val shipperId: String = "",
) {
    val isTerminal: Boolean
        get() = orderStatus in setOf(OrderStatusValues.DELIVERED, OrderStatusValues.CANCELLED)
}

object OrderStatusValues {
    const val PENDING = "PENDING"
    const val CONFIRMED = "CONFIRMED"
    const val PREPARING = "PREPARING"
    const val BAKING = "BAKING"
    const val READY = "READY"
    const val READY_FOR_DELIVERY = "READY_FOR_DELIVERY"
    const val READY_TO_DELIVER = "READY_TO_DELIVER"
    const val ASSIGNED_TO_SHIPPER = "ASSIGNED_TO_SHIPPER"
    const val DELIVERING = "DELIVERING"
    const val DELIVERED = "DELIVERED"
    const val CANCELLED = "CANCELLED"

    val shipperStartStatuses: Set<String> = setOf(
        READY,
        READY_FOR_DELIVERY,
        READY_TO_DELIVER,
        ASSIGNED_TO_SHIPPER,
    )
}
