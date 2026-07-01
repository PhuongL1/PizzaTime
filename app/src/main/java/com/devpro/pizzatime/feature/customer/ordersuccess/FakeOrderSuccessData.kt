package com.devpro.pizzatime.feature.customer.ordersuccess

import com.devpro.pizzatime.R

object FakeOrderSuccessData {

    fun getOrderSuccess(orderId: String): OrderSuccessUiModel {
        return OrderSuccessUiModel(
            orderId = orderId.ifBlank { DEFAULT_ORDER_ID },
            title = "Order Confirmed",
            message = "Your artisan creation is being prepared with care in our wood-fired oven.",
            estimatedArrival = "25-35 min",
            statusLabel = "Preparing",
            heroImageRes = R.drawable.img_pizza_time,
        )
    }

    private const val DEFAULT_ORDER_ID = "PT-9823"
}