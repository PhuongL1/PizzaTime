package com.devpro.pizzatime.feature.kitchen.board

enum class KitchenOrderStatus {
    WAITING,
    PREPARING,
    READY,
    NEW,
}

data class KitchenOrderUiModel(
    val orderId: String,
    val displayOrderCode: String = orderId,
    val fulfillmentLabel: String,
    val timeLabel: String,
    val status: KitchenOrderStatus,
    val items: List<KitchenOrderItemUiModel>,
    val note: String? = null,
    val progressLabel: String? = null,
    val progressPercent: Int = 20,
)

data class KitchenOrderItemUiModel(
    val quantity: Int,
    val name: String,
    val sizeLabel: String,
    val modifier: String? = null,
    val crust: String? = null,
)
