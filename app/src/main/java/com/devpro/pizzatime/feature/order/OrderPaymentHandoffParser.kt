package com.devpro.pizzatime.feature.order

import java.util.Locale

object OrderPaymentHandoffParser {

    const val FIELD_PAYMENT_METHOD = "paymentMethod"
    const val FIELD_PAYMENT_STATUS = "paymentStatus"
    const val FIELD_PAYMENT_PROVIDER = "paymentProvider"
    const val FIELD_PAYMENT_ATTEMPT_ID = "paymentAttemptId"
    const val FIELD_PAYMENT_REFERENCE = "paymentReference"
    const val FIELD_PAID_AT = "paidAt"
    const val FIELD_PROVIDER_TRANSACTION_ID = "providerTransactionId"
    const val FIELD_DELIVERY_HANDOFF_STATUS = "deliveryHandoffStatus"
    const val FIELD_SHIPPER_ARRIVED_AT = "shipperArrivedAt"
    const val FIELD_CUSTOMER_RECEIVED_AT = "customerReceivedAt"
    const val FIELD_CUSTOMER_RECEIPT_CONFIRMED_BY = "customerReceiptConfirmedBy"
    const val FIELD_DELIVERY_COMPLETED_AT = "deliveryCompletedAt"

    fun parse(
        orderStatus: String?,
        paymentMethodValue: String?,
        paymentStatusValue: String?,
        handoffStatusValue: String?,
        customerId: String?,
        shipperId: String?,
    ): OrderPaymentHandoffSnapshot {
        val paymentMethod = parsePaymentMethod(paymentMethodValue)
        return OrderPaymentHandoffSnapshot(
            orderStatus = orderStatus.orEmpty().trim().uppercase(Locale.US),
            paymentMethod = paymentMethod,
            paymentStatus = parsePaymentStatus(paymentMethod, paymentStatusValue),
            deliveryHandoffStatus = parseDeliveryHandoffStatus(paymentMethod, handoffStatusValue),
            customerId = customerId.orEmpty(),
            shipperId = shipperId.orEmpty(),
        )
    }

    fun parsePaymentMethod(value: String?): PaymentMethod {
        return when (value.orEmpty().trim().uppercase(Locale.US)) {
            "", "CASH_ON_DELIVERY", "CASH", "COD" -> PaymentMethod.COD
            "DEMO" -> PaymentMethod.DEMO
            "VNPAY" -> PaymentMethod.VNPAY
            else -> PaymentMethod.UNKNOWN
        }
    }

    fun parsePaymentStatus(
        method: PaymentMethod,
        value: String?,
    ): PaymentStatus {
        if (method == PaymentMethod.COD) {
            return PaymentStatus.NOT_REQUIRED
        }
        return when (value.orEmpty().trim().uppercase(Locale.US)) {
            "" -> if (method.isPrepaid()) PaymentStatus.PENDING else PaymentStatus.UNKNOWN
            "NOT_REQUIRED", "UNPAID" -> if (method.isPrepaid()) PaymentStatus.PENDING else PaymentStatus.NOT_REQUIRED
            "PENDING" -> PaymentStatus.PENDING
            "PAID" -> PaymentStatus.PAID
            "FAILED" -> PaymentStatus.FAILED
            "EXPIRED" -> PaymentStatus.EXPIRED
            "REFUNDED" -> PaymentStatus.REFUNDED
            else -> PaymentStatus.UNKNOWN
        }
    }

    fun parseDeliveryHandoffStatus(
        method: PaymentMethod,
        value: String?,
    ): DeliveryHandoffStatus {
        if (method == PaymentMethod.COD) {
            return DeliveryHandoffStatus.NOT_REQUIRED
        }
        return when (value.orEmpty().trim().uppercase(Locale.US)) {
            "" -> if (method.isPrepaid()) DeliveryHandoffStatus.LOCKED else DeliveryHandoffStatus.UNKNOWN
            "NOT_REQUIRED" -> DeliveryHandoffStatus.NOT_REQUIRED
            "LOCKED" -> DeliveryHandoffStatus.LOCKED
            "AWAITING_CUSTOMER" -> DeliveryHandoffStatus.AWAITING_CUSTOMER
            "CUSTOMER_CONFIRMED" -> DeliveryHandoffStatus.CUSTOMER_CONFIRMED
            "COMPLETED" -> DeliveryHandoffStatus.COMPLETED
            else -> DeliveryHandoffStatus.UNKNOWN
        }
    }

    fun codCreateFields(): Map<String, Any> {
        return mapOf(
            FIELD_PAYMENT_METHOD to PaymentMethod.COD.name,
            FIELD_PAYMENT_STATUS to PaymentStatus.NOT_REQUIRED.name,
            FIELD_DELIVERY_HANDOFF_STATUS to DeliveryHandoffStatus.NOT_REQUIRED.name,
            "cashCollected" to false,
        )
    }

    fun futureVnpayCreateFields(): Map<String, Any> {
        return mapOf(
            FIELD_PAYMENT_METHOD to PaymentMethod.VNPAY.name,
            FIELD_PAYMENT_STATUS to PaymentStatus.PENDING.name,
            FIELD_DELIVERY_HANDOFF_STATUS to DeliveryHandoffStatus.LOCKED.name,
            "cashCollected" to false,
        )
    }

    fun futureDemoCreateFields(): Map<String, Any> {
        return mapOf(
            FIELD_PAYMENT_METHOD to PaymentMethod.DEMO.name,
            FIELD_PAYMENT_STATUS to PaymentStatus.PENDING.name,
            FIELD_DELIVERY_HANDOFF_STATUS to DeliveryHandoffStatus.LOCKED.name,
            "cashCollected" to false,
        )
    }
}
