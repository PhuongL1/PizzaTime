package com.devpro.pizzatime.feature.customer.orderdetail

import androidx.annotation.DrawableRes

data class CustomerOrderDetailUiModel(
    val orderId: String,
    val statusLabel: String,
    val orderTime: String,
    @param:DrawableRes val heroImageRes: Int,
    val heroMessage: String,
    val items: List<CustomerOrderItemUiModel>,
    val bill: CustomerBillUiModel,
    val deliveryAddressTitle: String,
    val deliveryAddressLine1: String,
    val deliveryAddressLine2: String,
    val statusHistory: List<CustomerOrderStatusHistoryUiModel> = emptyList(),
    val canCancel: Boolean = false,
)

data class CustomerOrderItemUiModel(
    val quantity: Int,
    val name: String,
    val description: String,
    val price: Double,
    @param:DrawableRes val imageRes: Int?,
)

data class CustomerBillUiModel(
    val subtotal: Double,
    val deliveryFee: Double,
    val taxes: Double,
    val discountLabel: String,
    val discount: Double,
    val total: Double,
)
