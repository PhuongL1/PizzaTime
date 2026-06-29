package com.devpro.pizzatime.feature.staff.dashboard

object FakeStaffDashboardData {

    private val orders = mutableListOf(
        StaffOrderUiModel(
            orderId = "#PT-9824",
            customerName = "Alessandro Rossi",
            timeAgo = "2 MINS AGO",
            fulfillmentType = StaffFulfillmentType.DELIVERY,
            orderSummary = "1x Truffle Mushroom Pizza, 1x Sparkling Water",
            price = "$34.50",
            status = StaffOrderStatus.PENDING,
        ),
        StaffOrderUiModel(
            orderId = "#PT-9822",
            customerName = "Elena Vance",
            timeAgo = "8 MINS AGO",
            fulfillmentType = StaffFulfillmentType.COLLECTION,
            orderSummary = "2x Margherita Extra, 1x Tiramisu",
            price = "$42.00",
            status = StaffOrderStatus.PENDING,
        ),
        StaffOrderUiModel(
            orderId = "#PT-9821",
            customerName = "Marcus Thorne",
            timeAgo = "15 MINS AGO",
            fulfillmentType = StaffFulfillmentType.DELIVERY,
            orderSummary = "1x Spicy Salami Pizza, 1x Garlic Bread",
            price = "$28.90",
            status = StaffOrderStatus.CONFIRMED,
        ),
        StaffOrderUiModel(
            orderId = "#PT-9819",
            customerName = "Sofia Bianchi",
            timeAgo = "22 MINS AGO",
            fulfillmentType = StaffFulfillmentType.COLLECTION,
            orderSummary = "1x Burrata Pizza, 2x Lemon Soda",
            price = "$31.70",
            status = StaffOrderStatus.PREPARING,
        ),
        StaffOrderUiModel(
            orderId = "#PT-9815",
            customerName = "Luca Morgan",
            timeAgo = "31 MINS AGO",
            fulfillmentType = StaffFulfillmentType.DELIVERY,
            orderSummary = "1x Prosciutto Pizza, 1x Tiramisu",
            price = "$39.20",
            status = StaffOrderStatus.READY,
        ),
    )

    fun getOrdersByStatus(status: StaffOrderStatus): List<StaffOrderUiModel> {
        return orders.filter { order -> order.status == status }
    }

    fun confirmOrder(orderId: String) {
        val index = orders.indexOfFirst { order -> order.orderId == orderId }
        if (index == -1) return

        val currentOrder = orders[index]
        orders[index] = currentOrder.copy(status = StaffOrderStatus.CONFIRMED)
    }
}