package com.devpro.pizzatime.feature.customer.home

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.math.roundToInt

object CustomerHomeBestSellerRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val deliveredStatus = "DELIVERED"
    private val cancelledStatuses = setOf("CANCELLED", "CANCELED")

    fun loadBestSellingProductIds(onResult: (Result<List<String>>) -> Unit) {
        firestore.collection("orders")
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents
                val deliveredOrders = orders.filter { it.statusValue() == deliveredStatus }
                val sourceOrders = when {
                    deliveredOrders.isNotEmpty() -> deliveredOrders
                    orders.any { it.statusValue() !in cancelledStatuses } -> {
                        orders.filter { it.statusValue() !in cancelledStatuses }
                    }
                    else -> orders
                }
                onResult(Result.success(sourceOrders.toBestSellingProductIds()))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun List<DocumentSnapshot>.toBestSellingProductIds(): List<String> {
        val totals = linkedMapOf<String, Int>()
        forEach { doc ->
            val rawItems = doc.get("items") as? List<*> ?: return@forEach
            rawItems.forEach { rawItem ->
                val item = rawItem as? Map<*, *> ?: return@forEach
                val productId = (item["productId"] as? String).orEmpty()
                if (productId.isBlank()) return@forEach
                val quantity = item["quantity"].numberValue().roundToInt().coerceAtLeast(0)
                if (quantity == 0) return@forEach
                totals[productId] = (totals[productId] ?: 0) + quantity
            }
        }

        return totals.entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    private fun DocumentSnapshot.statusValue(): String {
        return (getString("status") ?: "").uppercase(Locale.US)
    }

    private fun Any?.numberValue(): Double {
        return when (this) {
            is Number -> toDouble()
            else -> 0.0
        }
    }
}
