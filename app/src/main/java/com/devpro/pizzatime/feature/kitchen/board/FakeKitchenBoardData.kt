package com.devpro.pizzatime.feature.kitchen.board

object FakeKitchenBoardData {

    fun getOrders(): List<KitchenOrderUiModel> = listOf(
        KitchenOrderUiModel(
            orderId = "ORD-7721",
            fulfillmentLabel = "DINE-IN · TABLE 14",
            timeLabel = "12m",
            status = KitchenOrderStatus.WAITING,
            items = listOf(
                KitchenOrderItemUiModel(
                    quantity = 1,
                    name = "Truffle Noir",
                    sizeLabel = "LARGE",
                    modifier = "NO ONIONS",
                    crust = "Thin Crust Artisan",
                ),
                KitchenOrderItemUiModel(
                    quantity = 2,
                    name = "Midnight Pepperoni",
                    sizeLabel = "MED",
                    modifier = "EXTRA JALAPEÑOS",
                    crust = "Traditional Copper-Fired",
                ),
            ),
        ),
        KitchenOrderUiModel(
            orderId = "ORD-7724",
            fulfillmentLabel = "DELIVERY · UBEREATS",
            timeLabel = "4:19",
            status = KitchenOrderStatus.PREPARING,
            progressLabel = "80% COMPLETE",
            items = listOf(
                KitchenOrderItemUiModel(
                    quantity = 1,
                    name = "Tuscan Garden",
                    sizeLabel = "MED",
                    modifier = "ALLERGY: GLUTEN",
                    crust = "Gluten Free Base",
                ),
            ),
        ),
        KitchenOrderUiModel(
            orderId = "ORD-7719",
            fulfillmentLabel = "TAKEAWAY · WEB",
            timeLabel = "",
            status = KitchenOrderStatus.READY,
            note = "3 Items Ready\nStation 4 - Heat Racô",
            items = emptyList(),
        ),
        KitchenOrderUiModel(
            orderId = "ORD-7728",
            fulfillmentLabel = "NEW ORDER",
            timeLabel = "--:--",
            status = KitchenOrderStatus.NEW,
            items = emptyList(),
        ),
    )
}