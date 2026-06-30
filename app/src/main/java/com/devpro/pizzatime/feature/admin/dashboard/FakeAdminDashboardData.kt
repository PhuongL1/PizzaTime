package com.devpro.pizzatime.feature.admin.dashboard

object FakeAdminDashboardData {

    fun getDashboard(): AdminDashboardUiModel {
        return AdminDashboardUiModel(
            totalRevenue = "$4,250.00",
            revenueGrowth = "↗12%",
            todayTotal = "142",
            pendingCount = "8",
            completedCount = "124 Completed",
            satisfactionLabel = "98% Satisfaction tonight",
            recentOrders = listOf(
                AdminRecentOrderUiModel(
                    orderId = "Order #8842",
                    summary = "2x Truffle Pepperoni, 1x Nero Soda",
                    price = "$42.00",
                ),
                AdminRecentOrderUiModel(
                    orderId = "Order #8841",
                    summary = "1x Midnight Margherita",
                    price = "$18.50",
                ),
                AdminRecentOrderUiModel(
                    orderId = "Order #8840",
                    summary = "3x Smoked Burrata, 2x Basil Oil",
                    price = "$64.20",
                ),
            ),
        )
    }
}