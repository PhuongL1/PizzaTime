package com.devpro.pizzatime.feature.order

object OrderPaymentHandoffPolicy {

    fun canStaffConfirmOrder(order: OrderPaymentHandoffSnapshot): Boolean {
        if (order.isTerminal || order.orderStatus != OrderStatusValues.PENDING) {
            return false
        }
        return when (order.paymentMethod) {
            PaymentMethod.COD -> true
            PaymentMethod.DEMO, PaymentMethod.VNPAY -> order.paymentStatus == PaymentStatus.PAID
            PaymentMethod.UNKNOWN -> false
        }
    }

    fun canShipperStartDelivery(
        order: OrderPaymentHandoffSnapshot,
        actingUid: String?,
    ): Boolean {
        if (actingUid.isNullOrBlank() || order.isTerminal) {
            return false
        }
        if (order.shipperId.isNotBlank() && order.shipperId != actingUid) {
            return false
        }
        if (order.orderStatus !in OrderStatusValues.shipperStartStatuses) {
            return false
        }
        return when (order.paymentMethod) {
            PaymentMethod.COD -> true
            PaymentMethod.DEMO, PaymentMethod.VNPAY -> order.paymentStatus == PaymentStatus.PAID
            PaymentMethod.UNKNOWN -> false
        }
    }

    fun canShipperMarkArrived(
        order: OrderPaymentHandoffSnapshot,
        actingUid: String?,
    ): Boolean {
        return actingUid != null &&
            actingUid == order.shipperId &&
            order.paymentMethod.isPrepaid() &&
            order.paymentStatus == PaymentStatus.PAID &&
            order.orderStatus == OrderStatusValues.DELIVERING &&
            order.deliveryHandoffStatus == DeliveryHandoffStatus.LOCKED
    }

    fun canCustomerConfirmReceipt(
        order: OrderPaymentHandoffSnapshot,
        actingUid: String?,
    ): Boolean {
        return actingUid != null &&
            actingUid == order.customerId &&
            order.paymentMethod.isPrepaid() &&
            order.paymentStatus == PaymentStatus.PAID &&
            order.orderStatus == OrderStatusValues.DELIVERING &&
            order.deliveryHandoffStatus == DeliveryHandoffStatus.AWAITING_CUSTOMER
    }

    fun canShipperCompleteDelivery(
        order: OrderPaymentHandoffSnapshot,
        actingUid: String?,
    ): Boolean {
        if (actingUid.isNullOrBlank() || actingUid != order.shipperId) {
            return false
        }
        if (order.orderStatus != OrderStatusValues.DELIVERING || order.isTerminal) {
            return false
        }
        return when (order.paymentMethod) {
            PaymentMethod.COD -> true
            PaymentMethod.DEMO, PaymentMethod.VNPAY -> {
                order.paymentStatus == PaymentStatus.PAID &&
                    order.deliveryHandoffStatus == DeliveryHandoffStatus.CUSTOMER_CONFIRMED
            }
            PaymentMethod.UNKNOWN -> false
        }
    }

    fun requiresCustomerReceipt(order: OrderPaymentHandoffSnapshot): Boolean {
        return order.paymentMethod.isPrepaid() && order.paymentStatus == PaymentStatus.PAID
    }

    fun shouldShowCustomerReceiptAction(
        order: OrderPaymentHandoffSnapshot,
        actingUid: String?,
    ): Boolean {
        return actingUid != null &&
            actingUid == order.customerId &&
            requiresCustomerReceipt(order) &&
            order.orderStatus in setOf(OrderStatusValues.DELIVERING, OrderStatusValues.DELIVERED)
    }

    fun shouldShowShipperArrivalAction(
        order: OrderPaymentHandoffSnapshot,
        actingUid: String?,
    ): Boolean {
        return actingUid != null &&
            actingUid == order.shipperId &&
            requiresCustomerReceipt(order) &&
            order.orderStatus == OrderStatusValues.DELIVERING
    }

    fun shouldShowShipperCompleteAction(
        order: OrderPaymentHandoffSnapshot,
        actingUid: String?,
    ): Boolean {
        return actingUid != null &&
            actingUid == order.shipperId &&
            order.orderStatus == OrderStatusValues.DELIVERING
    }
}
