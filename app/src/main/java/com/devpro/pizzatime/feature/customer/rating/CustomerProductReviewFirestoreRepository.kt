package com.devpro.pizzatime.feature.customer.rating

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale

object CustomerProductReviewFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadOrderRatings(
        orderId: String,
        customerId: String,
        onResult: (Result<Map<String, Int>>) -> Unit,
    ) {
        resolveOrderDocument(
            orderId = orderId,
            customerId = customerId,
        ) { resolved ->
            resolved
                .onSuccess { orderSnapshot ->
                    firestore.collection("productReviews")
                        .whereEqualTo("orderId", orderSnapshot.id)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val ratings = snapshot.documents.mapNotNull { doc ->
                                if (doc.getString("customerId") != customerId) {
                                    return@mapNotNull null
                                }
                                val productId = doc.getString("productId")?.trim().orEmpty()
                                if (productId.isBlank()) {
                                    return@mapNotNull null
                                }
                                val rating = doc.getLong("rating")?.toInt()
                                    ?: doc.getDouble("rating")?.toInt()
                                    ?: return@mapNotNull null
                                productId to rating
                            }.toMap()
                            onResult(Result.success(ratings))
                        }
                        .addOnFailureListener { error ->
                            Log.e(
                                TAG,
                                "Could not load order ratings",
                                error,
                            )
                            onResult(Result.failure(error))
                        }
                }
                .onFailure { error ->
                    Log.e(
                        TAG,
                        "Could not resolve order for ratings",
                        error,
                    )
                    onResult(Result.failure(error))
                }
        }
    }

    fun submitOrderRatings(
        orderId: String,
        customerId: String,
        ratings: Map<String, Int>,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val rawOrderId = orderId.trim()
        val normalizedOrderId = normalizeOrderLookupKey(orderId)
        val sanitizedRatings = ratings.entries
            .mapNotNull { entry ->
                val productId = entry.key.trim()
                val rating = entry.value
                if (productId.isBlank() || rating !in 1..5) {
                    if (productId.isBlank()) {
                        Log.w(TAG, "Order rating skipped because a product identifier is missing")
                    }
                    null
                } else {
                    productId to rating
                }
            }
            .distinctBy { it.first }
            .toMap()

        if (sanitizedRatings.isEmpty()) {
            onResult(Result.failure(Exception("Select a rating.")))
            return
        }

        resolveOrderDocument(
            orderId = rawOrderId,
            customerId = customerId,
        ) { resolved ->
            resolved
                .onSuccess { orderSnapshot ->
                    val orderCustomerId = orderSnapshot.getString("customerId").orEmpty()
                    val orderStatus = orderSnapshot.getString("status")?.uppercase(Locale.US).orEmpty()
                    if (orderCustomerId != customerId || orderStatus != STATUS_DELIVERED) {
                        val error = IllegalStateException("Delivered order validation failed.")
                        Log.e(
                            TAG,
                            "Order rating validation failed",
                            error,
                        )
                        onResult(Result.failure(error))
                        return@onSuccess
                    }

                    val batch = firestore.batch()
                    sanitizedRatings.forEach { (productId, rating) ->
                        val reviewId = buildReviewId(orderSnapshot.id, productId, customerId)
                        val reviewRef = firestore.collection("productReviews").document(reviewId)
                        batch.set(
                            reviewRef,
                            mapOf(
                                "reviewId" to reviewId,
                                "orderId" to orderSnapshot.id,
                                "productId" to productId,
                                "customerId" to customerId,
                                "rating" to rating,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                            SetOptions.merge(),
                        )
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            updateProductAggregatesBestEffort(
                                productIds = sanitizedRatings.keys.toList(),
                            )
                            onResult(Result.success(Unit))
                        }
                        .addOnFailureListener { error ->
                            Log.e(
                                TAG,
                                "Could not submit order ratings",
                                error,
                            )
                            onResult(Result.failure(error))
                        }
                }
                .onFailure { error ->
                    Log.e(
                        TAG,
                        "Could not resolve order before submitting ratings",
                        error,
                    )
                    onResult(Result.failure(error))
                }
        }
    }

    private fun updateProductAggregatesBestEffort(
        productIds: List<String>,
    ) {
        productIds.distinct().forEach { productId ->
            refreshProductAggregate(productId)
        }
    }

    private fun refreshProductAggregate(productId: String) {
        firestore.collection("productReviews")
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.documents.mapNotNull { document ->
                    document.getLong("rating")?.toInt()
                        ?: document.getDouble("rating")?.toInt()
                }.filter { it in 1..5 }

                val total = ratings.sum()
                val count = ratings.size
                val average = if (count > 0) total.toDouble() / count else 0.0
                val productRef = firestore.collection("products").document(productId)

                productRef.get()
                    .addOnSuccessListener { productSnapshot ->
                        if (!productSnapshot.exists()) {
                            Log.w(
                                TAG,
                                "Product rating aggregate skipped because product is unavailable",
                            )
                            return@addOnSuccessListener
                        }

                        productRef.set(
                            mapOf(
                                "averageRating" to average,
                                "ratingCount" to count,
                                "ratingTotal" to total,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                            SetOptions.merge(),
                        ).addOnFailureListener { error ->
                            Log.e(
                                TAG,
                                "Could not update product rating aggregate",
                                error,
                            )
                        }
                    }
                    .addOnFailureListener { error ->
                        Log.e(
                            TAG,
                            "Could not read product for rating aggregate",
                            error,
                        )
                    }
            }
            .addOnFailureListener { error ->
                Log.e(
                    TAG,
                    "Could not read product reviews for aggregate",
                    error,
                )
            }
    }

    private fun resolveOrderDocument(
        orderId: String,
        customerId: String,
        onResult: (Result<DocumentSnapshot>) -> Unit,
    ) {
        val rawOrderId = orderId.trim()
        val normalizedOrderId = normalizeOrderLookupKey(orderId)
        val directCandidates = linkedSetOf<String>().apply {
            if (normalizedOrderId.isNotBlank()) add(normalizedOrderId)
            if (rawOrderId.isNotBlank()) add(rawOrderId)
        }.toList()

        fun tryDirect(index: Int) {
            if (index >= directCandidates.size) {
                tryFieldFallbacks(
                    normalizedOrderId = normalizedOrderId,
                    rawOrderId = rawOrderId,
                    customerId = customerId,
                    onResult = onResult,
                )
                return
            }

            val candidate = directCandidates[index]
            firestore.collection("orders").document(candidate)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        onResult(Result.success(snapshot))
                    } else {
                        tryDirect(index + 1)
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(
                        TAG,
                        "Direct order lookup failed while resolving ratings",
                        error,
                    )
                    tryDirect(index + 1)
                }
        }

        tryDirect(index = 0)
    }

    private fun tryFieldFallbacks(
        normalizedOrderId: String,
        rawOrderId: String,
        customerId: String,
        onResult: (Result<DocumentSnapshot>) -> Unit,
    ) {
        val displayOrderCode = when {
            rawOrderId.startsWith("#") -> rawOrderId
            normalizedOrderId.isNotBlank() -> "#$normalizedOrderId"
            else -> rawOrderId.takeIf { it.isNotBlank() }.orEmpty()
        }

        val lookups = listOf(
            "orderId" to normalizedOrderId,
            "orderCodeKey" to normalizedOrderId,
            "orderCode" to displayOrderCode,
        ).filter { it.second.isNotBlank() }

        fun tryLookup(index: Int) {
            if (index >= lookups.size) {
                val error = IllegalStateException("Delivered order not found.")
                Log.e(
                    TAG,
                    "Order lookup failed while resolving ratings",
                    error,
                )
                onResult(Result.failure(error))
                return
            }

            val (field, value) = lookups[index]
            firestore.collection("orders")
                .whereEqualTo(field, value)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val match = snapshot.documents.firstOrNull()
                    if (match != null) {
                        onResult(Result.success(match))
                    } else {
                        tryLookup(index + 1)
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(
                        TAG,
                        "Fallback order lookup failed while resolving ratings",
                        error,
                    )
                    tryLookup(index + 1)
                }
        }

        tryLookup(index = 0)
    }

    private fun normalizeOrderLookupKey(orderId: String): String {
        return orderId.trim().removePrefix("#")
    }

    private fun buildReviewId(orderId: String, productId: String, customerId: String): String {
        return listOf(orderId, productId, customerId)
            .joinToString("_")
            .replace(Regex("[^A-Za-z0-9_\\-]"), "_")
    }

    private const val STATUS_DELIVERED = "DELIVERED"
    private const val TAG = "ProductReviewRepo"
}
