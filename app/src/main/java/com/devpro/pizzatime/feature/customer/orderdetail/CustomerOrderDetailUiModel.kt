package com.devpro.pizzatime.feature.customer.orderdetail

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.feature.order.DeliveryHandoffStatus
import com.devpro.pizzatime.feature.order.PaymentMethod
import com.devpro.pizzatime.feature.order.PaymentStatus

data class CustomerOrderDetailUiModel(
    val orderId: String,
    val displayOrderCode: String = orderId,
    val statusLabel: String,
    val orderTime: String,
    @param:DrawableRes val heroImageRes: Int,
    val heroImageUrl: String = "",
    val heroMessage: String,
    val items: List<CustomerOrderItemUiModel>,
    val bill: CustomerBillUiModel,
    val deliveryAddressTitle: String,
    val deliveryAddressLine1: String,
    val deliveryAddressLine2: String,
    val storeName: String = "",
    val pickupAddress: String = "",
    val storePhone: String = "",
    val distanceKm: Double? = null,
    val paymentMethod: String = "",
    val paymentStatus: String = "",
    val paymentMethodValue: PaymentMethod = PaymentMethod.COD,
    val paymentStatusValue: PaymentStatus = PaymentStatus.NOT_REQUIRED,
    val deliveryHandoffStatusValue: DeliveryHandoffStatus = DeliveryHandoffStatus.NOT_REQUIRED,
    val statusHistory: List<CustomerOrderStatusHistoryUiModel> = emptyList(),
    val canCancel: Boolean = false,
    val canConfirmReceipt: Boolean = false,
    val shouldShowReceiptAction: Boolean = false,
    val isReceiptConfirmed: Boolean = false,
)

data class CustomerOrderItemUiModel(
    val productId: String,
    val quantity: Int,
    val name: String,
    val description: String,
    val price: Double,
    @param:DrawableRes val imageRes: Int?,
    val imageUrl: String = "",
)

data class CustomerBillUiModel(
    val subtotal: Double,
    val deliveryFee: Double,
    val taxes: Double,
    val discountLabel: String,
    val discount: Double,
    val total: Double,
)
