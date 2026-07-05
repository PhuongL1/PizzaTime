package com.devpro.pizzatime.feature.admin.dashboard

data class AdminDashboardUiModel(
    val totalRevenue: String,
    val revenueGrowth: String,
    val todayTotal: String,
    val pendingCount: String,
    val completedCount: String,
    val satisfactionLabel: String,
    val recentOrders: List<AdminRecentOrderUiModel>,
)

data class AdminRecentOrderUiModel(
    val orderId: String,
    val displayOrderCode: String = orderId,
    val summary: String,
    val price: String,
)
