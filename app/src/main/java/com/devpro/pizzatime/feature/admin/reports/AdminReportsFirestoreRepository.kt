package com.devpro.pizzatime.feature.admin.reports

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

object AdminReportsFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val deliveredStatus = "DELIVERED"
    private val cancelledStatuses = setOf("CANCELLED", "CANCELED")
    private val finalStatuses = cancelledStatuses + deliveredStatus

    fun loadReports(onResult: (Result<AdminReportUiModel>) -> Unit) {
        firestore.collection("orders")
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(Result.success(snapshot.documents.toReport()))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    private fun List<DocumentSnapshot>.toReport(): AdminReportUiModel {
        val deliveredOrders = filter { it.statusValue() == deliveredStatus }
        val cancelledCount = count { it.statusValue() in cancelledStatuses }
        val pendingCount = count { it.statusValue() !in finalStatuses }
        val revenue = deliveredOrders.sumOf { it.numberValue("total") }
        val deliveredCount = deliveredOrders.size
        val totalCount = size
        val bestSellers = buildBestSellers()

        return AdminReportUiModel(
            totalRevenue = String.format(Locale.US, "$%.2f", revenue),
            totalOrdersText = "$totalCount total orders",
            pendingOrdersText = pendingCount.toString(),
            deliveredOrdersText = "$deliveredCount delivered",
            cancelledOrdersText = "$cancelledCount cancelled",
            pendingProgress = percent(pendingCount, totalCount),
            orderHealthPercent = percent(deliveredCount, totalCount),
            revenueTrendValues = buildRevenueTrend(deliveredOrders),
            bestSellers = bestSellers.ifEmpty { FakeAdminReportsData.bestSellers },
        )
    }

    private fun List<DocumentSnapshot>.buildBestSellers(): List<BestSellerUiModel> {
        val totals = linkedMapOf<String, BestSellerTotal>()
        forEach { doc ->
            val rawItems = doc.get("items") as? List<*> ?: return@forEach
            rawItems.forEach { rawItem ->
                val item = rawItem as? Map<*, *> ?: return@forEach
                val name = (item["name"] as? String).orEmpty().ifBlank { "Unknown item" }
                val productId = (item["productId"] as? String).orEmpty().ifBlank {
                    name.lowercase(Locale.US).replace(Regex("\\s+"), "_")
                }
                val quantity = item["quantity"].numberValue().roundToInt().coerceAtLeast(0)
                if (quantity == 0) return@forEach

                val current = totals[productId]
                totals[productId] = if (current == null) {
                    BestSellerTotal(productId, name, quantity)
                } else {
                    current.copy(quantity = current.quantity + quantity)
                }
            }
        }

        val maxQuantity = totals.values.maxOfOrNull { it.quantity } ?: 0
        return totals.values
            .sortedByDescending { it.quantity }
            .take(5)
            .mapIndexed { index, item ->
                BestSellerUiModel(
                    id = item.id,
                    rank = "#${index + 1}",
                    name = item.name,
                    soldText = "${item.quantity} SOLD",
                    progress = percent(item.quantity, maxQuantity),
                )
            }
    }

    private fun buildRevenueTrend(deliveredOrders: List<DocumentSnapshot>): List<Float> {
        if (deliveredOrders.isEmpty()) return List(TREND_BUCKET_COUNT) { 0f }

        val buckets = MutableList(TREND_BUCKET_COUNT) { 0.0 }
        deliveredOrders.forEach { doc ->
            val timestamp = doc.getTimestamp("createdAt")
            val bucketIndex = timestamp?.toDate()?.let { date ->
                val calendar = Calendar.getInstance().apply { time = date }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                (hour * TREND_BUCKET_COUNT / HOURS_PER_DAY).coerceIn(0, TREND_BUCKET_COUNT - 1)
            } ?: 0
            buckets[bucketIndex] += doc.numberValue("total")
        }

        val maxRevenue = buckets.maxOrNull() ?: 0.0
        if (maxRevenue <= 0.0) return List(TREND_BUCKET_COUNT) { 0f }
        return buckets.map { (it / maxRevenue).toFloat().coerceIn(0.05f, 1f) }
    }

    private fun DocumentSnapshot.statusValue(): String {
        return (getString("status") ?: "PENDING").uppercase(Locale.US)
    }

    private fun DocumentSnapshot.numberValue(field: String): Double {
        return get(field).numberValue()
    }

    private fun Any?.numberValue(): Double {
        return when (this) {
            is Number -> toDouble()
            else -> 0.0
        }
    }

    private fun percent(value: Int, max: Int): Int {
        if (max <= 0) return 0
        return (value * 100.0 / max).roundToInt().coerceIn(0, 100)
    }

    private data class BestSellerTotal(
        val id: String,
        val name: String,
        val quantity: Int,
    )

    private const val TREND_BUCKET_COUNT = 9
    private const val HOURS_PER_DAY = 24
}
