package com.devpro.pizzatime.feature.customer.home

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.math.roundToInt

object CustomerHomeBestSellerRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val cancelledStatuses = setOf("CANCELLED", "CANCELED")

    fun loadOrderedProductQuantities(onResult: (Result<Map<String, Int>>) -> Unit) {
        firestore.collection("orders")
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents
                val sourceOrders = orders.filterNot { it.statusValue() in cancelledStatuses }
                onResult(Result.success(sourceOrders.toOrderedProductQuantities()))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun List<DocumentSnapshot>.toOrderedProductQuantities(): Map<String, Int> {
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

        return totals.toMap()
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
