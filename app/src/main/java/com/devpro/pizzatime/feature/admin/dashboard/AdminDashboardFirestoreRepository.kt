package com.devpro.pizzatime.feature.admin.dashboard

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object AdminDashboardFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val DELIVERED_STATUS = "DELIVERED"
    private val FINAL_STATUSES = setOf("DELIVERED", "CANCELLED", "CANCELED")
    private const val RECENT_ORDER_LIMIT = 5

    fun loadDashboard(onResult: (Result<AdminDashboardUiModel>) -> Unit) {
        firestore.collection("orders")
            .get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents.sortedByDescending {
                    it.getTimestamp("createdAt")?.seconds ?: 0L
                }
                onResult(Result.success(docs.toDashboard()))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    private fun List<DocumentSnapshot>.toDashboard(): AdminDashboardUiModel {
        val deliveredDocs = filter {
            it.getString("status")?.uppercase(Locale.US) == DELIVERED_STATUS
        }
        val pendingCount = count {
            (it.getString("status")?.uppercase(Locale.US) ?: "PENDING") !in FINAL_STATUSES
        }
        val revenue = deliveredDocs.sumOf { it.getDouble("total") ?: 0.0 }

        return AdminDashboardUiModel(
            totalRevenue = String.format(Locale.US, "$%.2f", revenue),
            revenueGrowth = "",
            todayTotal = size.toString(),
            pendingCount = pendingCount.toString(),
            completedCount = "${deliveredDocs.size} Delivered",
            satisfactionLabel = "",
            recentOrders = take(RECENT_ORDER_LIMIT).map { it.toRecentOrder() },
        )
    }

    private fun DocumentSnapshot.toRecentOrder(): AdminRecentOrderUiModel {
        val shortId = id.takeLast(4).uppercase(Locale.US)
        val total = getDouble("total") ?: 0.0
        val rawItems = get("items") as? List<*>
        return AdminRecentOrderUiModel(
            orderId = "Order #$shortId",
            summary = buildSummary(rawItems),
            price = String.format(Locale.US, "$%.2f", total),
        )
    }

    private fun buildSummary(rawItems: List<*>?): String {
        if (rawItems.isNullOrEmpty()) return ""
        val lines = rawItems.take(3).mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val name = map["name"] as? String ?: return@mapNotNull null
            val qty = (map["quantity"] as? Long)?.toInt() ?: 1
            "${qty}x $name"
        }
        return if (rawItems.size > 3) {
            lines.joinToString(", ") + " +${rawItems.size - 3} more"
        } else {
            lines.joinToString(", ")
        }
    }
}

