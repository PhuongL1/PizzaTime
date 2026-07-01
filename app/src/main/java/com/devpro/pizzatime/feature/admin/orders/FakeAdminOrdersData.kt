package com.devpro.pizzatime.feature.admin.orders

object FakeAdminOrdersData {

    fun getOrders(): List<AdminOrderUiModel> {
        return listOf(
            AdminOrderUiModel(
                orderId = "MA-9283",
                customerName = "Julian Thorne",
                phone = "+1 (555) 010-9283",
                itemsSummary = "2x Black Truffle Artisan Pizza",
                total = 54.00,
                status = AdminOrderStatus.PENDING,
                metaText = "Ordered 4 mins ago",
            ),
            AdminOrderUiModel(
                orderId = "MA-9281",
                customerName = "Elena Vance",
                phone = "+1 (555) 010-9281",
                itemsSummary = "1x Prosciutto & Fig Pizza",
                total = 28.50,
                status = AdminOrderStatus.CONFIRMED,
                metaText = "In Kitchen (Prep)",
            ),
            AdminOrderUiModel(
                orderId = "MA-9275",
                customerName = "Marcus Wright",
                phone = "+1 (555) 010-9275",
                itemsSummary = "3x Spicy Calabrese, 2x Pinot Noir",
                total = 112.00,
                status = AdminOrderStatus.SHIPPED,
                metaText = "Shipper: Dave S. (ETA 12m)",
            ),
            AdminOrderUiModel(
                orderId = "MA-9280",
                customerName = "Sarah Chen",
                phone = "+1 (555) 010-9280",
                itemsSummary = "1x Wild Mushroom Risotto",
                total = 32.00,
                status = AdminOrderStatus.READY,
                metaText = "Awaiting Shipper Pickup",
            ),
        )
    }
}