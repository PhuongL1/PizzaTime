package com.devpro.pizzatime.feature.kitchen.board

enum class KitchenOrderStatus {
    WAITING,
    PREPARING,
    READY,
    NEW,
}

data class KitchenOrderUiModel(
    val orderId: String,
    val fulfillmentLabel: String,
    val timeLabel: String,
    val status: KitchenOrderStatus,
    val items: List<KitchenOrderItemUiModel>,
    val note: String? = null,
    val progressLabel: String? = null,
)

data class KitchenOrderItemUiModel(
    val quantity: Int,
    val name: String,
    val sizeLabel: String,
    val modifier: String? = null,
    val crust: String? = null,
)