package com.devpro.pizzatime.feature.customer.orderhistory

data class CustomerOrderHistoryUiModel(
    val title: String,
    val subtitle: String,
    val orders: List<CustomerOrderHistoryItemUiModel>,
    val reward: CustomerOrderRewardUiModel,
)

data class CustomerOrderHistoryItemUiModel(
    val orderId: String,
    val displayOrderCode: String = orderId,
    val orderedAt: String,
    val status: CustomerOrderHistoryStatus,
    val itemSummary: List<String>,
    val total: Double,
    val imageRes: Int?,
    val imageUrl: String = "",
    val heroProductId: String = "",
)

data class CustomerOrderRewardUiModel(
    val title: String,
    val description: String,
    val currentOrders: Int,
    val targetOrders: Int,
)

enum class CustomerOrderHistoryStatus(
    val label: String,
) {
    DELIVERED("DELIVERED"),
    CANCELED("CANCELLED"),
    IN_PROGRESS("IN PROGRESS"),
}

enum class CustomerOrderHistoryFilter {
    ALL,
    DELIVERED,
    CANCELED,
}
