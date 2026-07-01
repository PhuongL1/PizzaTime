package com.devpro.pizzatime.feature.customer.orderhistory

import com.devpro.pizzatime.R

object FakeCustomerOrderHistoryData {

    fun getOrderHistory(): CustomerOrderHistoryUiModel {
        return CustomerOrderHistoryUiModel(
            title = "Order History",
            subtitle = "Your late-night artisan chronicles.",
            orders = listOf(
                CustomerOrderHistoryItemUiModel(
                    orderId = "PT-8821",
                    orderedAt = "OCT 24, 2023 · 11:42 PM",
                    status = CustomerOrderHistoryStatus.DELIVERED,
                    itemSummary = listOf(
                        "1x Truffle Ember Neapolitan",
                        "2x Peroni Nastro Azzurro",
                    ),
                    total = 42.0,
                    imageRes = R.drawable.img_pizza_time,
                ),
                CustomerOrderHistoryItemUiModel(
                    orderId = "PT-7940",
                    orderedAt = "OCT 12, 2023 · 10:15 PM",
                    status = CustomerOrderHistoryStatus.CANCELED,
                    itemSummary = listOf(
                        "1x Hot Honey Soppressata",
                        "1x Burrata Salad",
                    ),
                    total = 38.5,
                    imageRes = null,
                ),
                CustomerOrderHistoryItemUiModel(
                    orderId = "PT-6211",
                    orderedAt = "SEP 29, 2023 · 01:05 AM",
                    status = CustomerOrderHistoryStatus.DELIVERED,
                    itemSummary = listOf(
                        "2x Midnight Garden Pizza",
                    ),
                    total = 56.0,
                    imageRes = R.drawable.img_pizza_time,
                ),
            ),
            reward = CustomerOrderRewardUiModel(
                title = "Order #5 is near!",
                description = "Place one more order to unlock \"The Bronze Oven\" tier benefits.",
                currentOrders = 4,
                targetOrders = 5,
            ),
        )
    }
}