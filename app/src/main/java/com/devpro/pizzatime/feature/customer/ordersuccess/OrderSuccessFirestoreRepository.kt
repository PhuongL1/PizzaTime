package com.devpro.pizzatime.feature.customer.ordersuccess

import android.util.Log
import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashSet
import java.util.Locale

object OrderSuccessFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadOrderSuccess(
        orderIdOrCode: String,
        onResult: (Result<OrderSuccessUiModel>) -> Unit,
    ) {
        loadOrderDocument(orderIdOrCode) { result ->
            result
                .onSuccess { orderDoc ->
                    val heroCandidate = orderDoc.toHeroCandidate()
                    loadHeroImageUrl(heroCandidate) { heroImageUrl ->
                        if (heroCandidate == null) {
                            Log.w(TAG, "Order has no items")
                        } else if (heroImageUrl.isBlank()) {
                            Log.w(
                                TAG,
                                "Hero image missing for orderId=${orderDoc.id}, productId=${heroCandidate.productId}",
                            )
                        }
                        onResult(Result.success(orderDoc.toOrderSuccessUiModel(heroImageUrl)))
                    }
                }
                .onFailure { error ->
                    onResult(Result.failure(error))
                }
        }
    }

    private fun loadOrderDocument(
        orderIdOrCode: String,
        onResult: (Result<DocumentSnapshot>) -> Unit,
    ) {
        val normalizedInput = orderIdOrCode.trim()
        val normalizedKey = normalizedInput.removePrefix("#")
        val orderCodeWithHash = if (normalizedKey.isBlank()) {
            ""
        } else {
            OrderCodeGenerator.displayOrderCode(normalizedKey)
        }

        val candidateDocumentIds = LinkedHashSet<String>().apply {
            add(normalizedInput)
            add(normalizedKey)
        }.filter { candidate -> candidate.isNotBlank() }

        fun queryByField(
            fieldQueries: List<Pair<String, String>>,
            index: Int,
        ) {
            if (index >= fieldQueries.size) {
                onResult(Result.failure(IllegalStateException("Order $orderIdOrCode not found.")))
                return
            }

            val (field, value) = fieldQueries[index]
            firestore.collection("orders")
                .whereEqualTo(field, value)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.documents.firstOrNull()
                    if (doc != null) {
                        onResult(Result.success(doc))
                    } else {
                        queryByField(fieldQueries, index + 1)
                    }
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "Order lookup failed for $field=$value", error)
                    queryByField(fieldQueries, index + 1)
                }
        }

        fun queryByDocumentId(index: Int) {
            if (index >= candidateDocumentIds.size) {
                queryByField(
                    fieldQueries = listOf(
                        "orderCodeKey" to normalizedKey,
                        "orderId" to normalizedKey,
                        "orderCode" to orderCodeWithHash,
                        "orderCode" to normalizedKey,
                    ).filter { (_, value) -> value.isNotBlank() },
                    index = 0,
                )
                return
            }

            val candidateId = candidateDocumentIds[index]
            firestore.collection("orders").document(candidateId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        onResult(Result.success(doc))
                    } else {
                        queryByDocumentId(index + 1)
                    }
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "Order lookup failed for documentId=$candidateId", error)
                    queryByDocumentId(index + 1)
                }
        }

        queryByDocumentId(index = 0)
    }

    private fun loadHeroImageUrl(
        heroCandidate: HeroCandidate?,
        onResult: (String) -> Unit,
    ) {
        if (heroCandidate == null) {
            onResult("")
            return
        }
        if (heroCandidate.imageUrl.isNotBlank()) {
            onResult(heroCandidate.imageUrl)
            return
        }
        if (heroCandidate.productId.isBlank()) {
            onResult("")
            return
        }

        firestore.collection("products").document(heroCandidate.productId)
            .get()
            .addOnSuccessListener { productDoc ->
                onResult(
                    productDoc.firstString("imageUrl", "productImageUrl", "photoUrl")
                        .trim(),
                )
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Product image lookup failed for productId=${heroCandidate.productId}", error)
                onResult("")
            }
    }

    private fun DocumentSnapshot.toOrderSuccessUiModel(heroImageUrl: String): OrderSuccessUiModel {
        val status = getString("status").orEmpty()
        return OrderSuccessUiModel(
            orderId = id,
            displayOrderCode = OrderCodeGenerator.displayOrderCode(
                orderCode = firstString("orderCode"),
                orderId = id,
            ),
            title = "Order Confirmed",
            message = "Your order was placed successfully and is now moving through the kitchen.",
            estimatedArrival = resolveEstimatedArrival(status, getTimestamp("createdAt")),
            statusLabel = status.toOrderSuccessStatusLabel(),
            heroImageUrl = heroImageUrl,
            heroImageRes = R.drawable.img_pizza_time,
        )
    }

    private fun DocumentSnapshot.toHeroCandidate(): HeroCandidate? {
        val items = get("items") as? List<*>
        return items
            ?.mapNotNull { it.toHeroCandidate() }
            ?.maxByOrNull { candidate -> candidate.lineTotal }
    }

    private fun Any?.toHeroCandidate(): HeroCandidate? {
        val map = this as? Map<*, *> ?: return null
        val quantity = map["quantity"].asIntOrNull()?.coerceAtLeast(1) ?: 1
        val lineTotal = map["totalPrice"].asDoubleOrNull()
            ?: map["lineTotal"].asDoubleOrNull()
            ?: map["unitPrice"].asDoubleOrNull()?.times(quantity)
            ?: map["price"].asDoubleOrNull()
            ?: 0.0
        if (lineTotal <= 0.0) {
            return null
        }
        return HeroCandidate(
            productId = map.firstString("productId", "id"),
            imageUrl = map.firstString("imageUrl", "productImageUrl", "photoUrl"),
            lineTotal = lineTotal,
        )
    }

    private fun resolveEstimatedArrival(
        status: String,
        createdAt: Timestamp?,
    ): String {
        val normalizedStatus = status.uppercase(Locale.US)
        return when (normalizedStatus) {
            "DELIVERED" -> "Delivered"
            "DELIVERING" -> "10-20 min"
            "READY" -> "Ready now"
            "BAKING", "PREPARING", "CONFIRMED", "PENDING", "" -> "25-35 min"
            else -> createdAt?.toEstimatedWindow() ?: "25-35 min"
        }
    }

    private fun Timestamp.toEstimatedWindow(): String {
        val baseTime = seconds * 1_000L
        val earliest = SimpleDateFormat("hh:mm a", Locale.US).format(Date(baseTime + 25 * 60_000L))
        val latest = SimpleDateFormat("hh:mm a", Locale.US).format(Date(baseTime + 35 * 60_000L))
        return "$earliest - $latest"
    }

    private fun String.toOrderSuccessStatusLabel(): String {
        return when (uppercase(Locale.US)) {
            "PENDING", "CONFIRMED", "PREPARING", "BAKING" -> "Preparing"
            "READY" -> "Ready"
            "DELIVERING" -> "Delivering"
            "DELIVERED" -> "Delivered"
            "CANCELLED", "CANCELED" -> "Cancelled"
            else -> ifBlank { "Preparing" }
        }
    }

    private fun DocumentSnapshot.firstString(vararg keys: String): String {
        keys.forEach { key ->
            val value = getString(key)
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    private fun Map<*, *>.firstString(vararg keys: String): String {
        keys.forEach { key ->
            val value = this[key] as? String
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    private fun Any?.asDoubleOrNull(): Double? {
        return when (this) {
            is Number -> toDouble()
            is String -> toDoubleOrNull()
            else -> null
        }
    }

    private fun Any?.asIntOrNull(): Int? {
        return when (this) {
            is Number -> toInt()
            is String -> toIntOrNull()
            else -> null
        }
    }

    private data class HeroCandidate(
        val productId: String,
        val imageUrl: String,
        val lineTotal: Double,
    )

    private const val TAG = "OrderSuccessRepo"
}
