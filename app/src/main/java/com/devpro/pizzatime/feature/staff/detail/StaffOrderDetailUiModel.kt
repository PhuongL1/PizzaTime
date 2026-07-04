package com.devpro.pizzatime.feature.staff.detail

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderStatus

data class StaffOrderDetailUiModel(
    val orderId: String,
    val receivedAgo: String,
    val status: StaffOrderStatus,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val estimatedDeliveryTime: String,
    val paymentMethod: String,
    val paymentStatus: String,
    val cashCollected: Boolean = false,
    val collectedByShipperId: String = "",
    val collectedAmount: Double = 0.0,
    val paymentTotal: Double,
    val deliveryNote: String,
    val items: List<StaffOrderDetailItemUiModel>,
    val timeline: StaffOrderDetailTimelineUiModel,
    val storeName: String = "",
    val pickupAddress: String = "",
    val storePhone: String = "",
    val distanceKm: Double? = null,
    val deliveryFee: Double = 0.0,
) {
    val mainItem: StaffOrderDetailItemUiModel?
        get() = items.firstOrNull()

    val itemCount: Int
        get() = items.sumOf { it.quantity }
}

data class StaffOrderDetailItemUiModel(
    val name: String,
    val description: String,
    val quantity: Int,
    val price: Double,
    @param:DrawableRes val imageRes: Int,
)

data class StaffOrderDetailTimelineUiModel(
    val orderPlacedTime: String,
    val confirmedTime: String?,
    val preparingTime: String?,
    val readyTime: String?,
)
