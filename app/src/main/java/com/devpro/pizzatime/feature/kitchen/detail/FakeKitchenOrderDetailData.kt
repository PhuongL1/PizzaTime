package com.devpro.pizzatime.feature.kitchen.detail

import com.devpro.pizzatime.R

object FakeKitchenOrderDetailData {

    private val statusOverrides = mutableMapOf<String, KitchenOrderDetailStatus>()

    fun getOrderDetail(orderId: String): KitchenOrderDetailUiModel {
        val normalizedOrderId = normalizeOrderId(orderId)
        val status = statusOverrides[normalizedOrderId] ?: KitchenOrderDetailStatus.PENDING

        return KitchenOrderDetailUiModel(
            orderId = normalizedOrderId,
            receivedAgo = "4m",
            status = status,
            item = KitchenOrderDetailItemUiModel(
                name = "TRUFFLE NOIR",
                size = "Large (16\")",
                crust = "Artisan Thin",
                toppings = listOf(
                    "Double Buffalo Mozzarella",
                    "Black Truffle Shavings",
                    "Wild Porcini Mushrooms",
                    "Roasted Garlic Confit",
                    "Fresh Chives",
                ),
                imageRes = R.drawable.img_pizza_time,
            ),
            allergyTitle = "ALLERGY: GLUTEN",
            allergyMessage = "Use separate preparation station and clean peel. Double check flour contamination.",
            customerRequest = "Extra truffle oil on the side and please char the crust slightly more than usual. It's for an anniversary dinner!",
            tags = listOf("VIP TIER", "ANNIVERSARY"),
        )
    }

    fun updateStatus(
        orderId: String,
        status: KitchenOrderDetailStatus,
    ) {
        statusOverrides[normalizeOrderId(orderId)] = status
    }

    private fun normalizeOrderId(orderId: String): String {
        return orderId
            .removePrefix("#")
            .trim()
            .ifBlank { DEFAULT_ORDER_ID }
    }

    private const val DEFAULT_ORDER_ID = "ORD-8824"
}