package com.devpro.pizzatime.feature.staff.detail

import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.customer.orderdetail.FakeCustomerOrderDetailData
import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderStatus

object FakeStaffOrderDetailData {

    private val statusOverrides = mutableMapOf<String, StaffOrderStatus>()

    fun getByOrderId(orderId: String): StaffOrderDetailUiModel {
        val normalizedOrderId = normalizeOrderId(orderId)
        val customerOrder = FakeCustomerOrderDetailData.getOrderDetail(normalizedOrderId)
        val status = statusOverrides[normalizedOrderId] ?: mapCustomerStatus(customerOrder.statusLabel)

        return StaffOrderDetailUiModel(
            orderId = customerOrder.orderId,
            receivedAgo = DEFAULT_RECEIVED_AGO,
            status = status,
            customerName = DEFAULT_CUSTOMER_NAME,
            customerPhone = DEFAULT_CUSTOMER_PHONE,
            deliveryAddress = buildDeliveryAddress(
                line1 = customerOrder.deliveryAddressLine1,
                line2 = customerOrder.deliveryAddressLine2,
            ),
            estimatedDeliveryTime = DEFAULT_ESTIMATED_DELIVERY_TIME,
            paymentMethod = DEFAULT_PAYMENT_METHOD,
            paymentTotal = customerOrder.bill.total,
            deliveryNote = DEFAULT_DELIVERY_NOTE,
            items = customerOrder.items.map { item ->
                StaffOrderDetailItemUiModel(
                    name = item.name,
                    description = item.description,
                    quantity = item.quantity,
                    price = item.price,
                    imageRes = item.imageRes ?: R.drawable.img_pizza_time,
                )
            },
            timeline = StaffOrderDetailTimelineUiModel(
                orderPlacedTime = customerOrder.orderTime,
                confirmedTime = DEFAULT_CONFIRMED_TIME,
                preparingTime = DEFAULT_PREPARING_TIME,
                readyTime = null,
            ),
        )
    }

    fun updateStatus(orderId: String, status: StaffOrderStatus) {
        statusOverrides[normalizeOrderId(orderId)] = status
    }

    private fun normalizeOrderId(orderId: String): String {
        return orderId
            .removePrefix("Order #")
            .removePrefix("#")
            .trim()
            .ifBlank { DEFAULT_ORDER_ID }
    }

    private fun mapCustomerStatus(statusLabel: String): StaffOrderStatus {
        return when (statusLabel.uppercase()) {
            STATUS_PENDING -> StaffOrderStatus.PENDING
            STATUS_CONFIRMED -> StaffOrderStatus.CONFIRMED
            STATUS_PREPARING -> StaffOrderStatus.PREPARING
            STATUS_READY,
            STATUS_DELIVERING,
            STATUS_DELIVERED,
                -> StaffOrderStatus.READY
            else -> StaffOrderStatus.CONFIRMED
        }
    }

    private fun buildDeliveryAddress(
        line1: String,
        line2: String,
    ): String {
        return listOf(line1, line2)
            .filter { it.isNotBlank() }
            .joinToString(separator = ", ")
    }

    private const val DEFAULT_ORDER_ID = "PT-9821"
    private const val DEFAULT_RECEIVED_AGO = "8 min"
    private const val DEFAULT_CUSTOMER_NAME = "Alessandra Rossi"
    private const val DEFAULT_CUSTOMER_PHONE = "+1 (555) 018-8291"
    private const val DEFAULT_ESTIMATED_DELIVERY_TIME = "11:45 PM"
    private const val DEFAULT_PAYMENT_METHOD = "Apple Pay"
    private const val DEFAULT_DELIVERY_NOTE = "Gate code is 1234. Please leave at the lobby desk if night concierge is present."
    private const val DEFAULT_CONFIRMED_TIME = "10:45 PM"
    private const val DEFAULT_PREPARING_TIME = "10:52 PM"

    private const val STATUS_PENDING = "PENDING"
    private const val STATUS_CONFIRMED = "CONFIRMED"
    private const val STATUS_PREPARING = "PREPARING"
    private const val STATUS_READY = "READY"
    private const val STATUS_DELIVERING = "DELIVERING"
    private const val STATUS_DELIVERED = "DELIVERED"
}