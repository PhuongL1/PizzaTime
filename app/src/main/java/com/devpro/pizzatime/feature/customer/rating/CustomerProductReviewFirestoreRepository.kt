package com.devpro.pizzatime.feature.customer.rating

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
        firestore.collection("productReviews")
            .whereEqualTo("orderId", orderId)
            .get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.documents.mapNotNull { doc ->
                    if (doc.getString("customerId") != customerId) {
                        return@mapNotNull null
                    }
                    val productId = doc.getString("productId") ?: return@mapNotNull null
                    val rating = doc.getLong("rating")?.toInt()
                        ?: doc.getDouble("rating")?.toInt()
                        ?: return@mapNotNull null
                    productId to rating
                }.toMap()
                onResult(Result.success(ratings))
            }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun submitOrderRatings(
        orderId: String,
        customerId: String,
        ratings: Map<String, Int>,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val sanitizedRatings = ratings.filterValues { it in 1..5 }
        if (sanitizedRatings.isEmpty()) {
            onResult(Result.failure(Exception("Select a rating.")))
            return
        }

        firestore.runTransaction { transaction ->
            val orderRef = firestore.collection("orders").document(orderId)
            val orderSnap = transaction.get(orderRef)
            val orderCustomerId = orderSnap.getString("customerId")
            val status = orderSnap.getString("status")?.uppercase(Locale.US).orEmpty()
            if (orderCustomerId != customerId || status != "DELIVERED") {
                throw IllegalStateException("Could not save rating.")
            }

            sanitizedRatings.forEach { (productId, rating) ->
                val reviewId = buildReviewId(orderId, productId, customerId)
                val reviewRef = firestore.collection("productReviews").document(reviewId)
                val productRef = firestore.collection("products").document(productId)

                val reviewSnap = transaction.get(reviewRef)
                val productSnap = transaction.get(productRef)

                val existingRating = reviewSnap.getLong("rating")?.toInt()
                val currentCount = (productSnap.getLong("ratingCount") ?: 0L).toInt().coerceAtLeast(0)
                val currentTotal = (productSnap.getLong("ratingTotal")
                    ?: productSnap.getDouble("ratingTotal")?.toLong()
                    ?: 0L).toInt().coerceAtLeast(0)

                val nextCount = if (existingRating == null) {
                    currentCount + 1
                } else {
                    currentCount.coerceAtLeast(1)
                }
                val nextTotal = when {
                    existingRating == null -> currentTotal + rating
                    currentTotal <= 0 -> rating
                    else -> (currentTotal - existingRating + rating).coerceAtLeast(rating)
                }
                val nextAverage = if (nextCount > 0) nextTotal.toDouble() / nextCount else 0.0

                val reviewData = linkedMapOf<String, Any?>(
                    "reviewId" to reviewId,
                    "orderId" to orderId,
                    "productId" to productId,
                    "customerId" to customerId,
                    "rating" to rating,
                    "createdAt" to (reviewSnap.get("createdAt") ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                transaction.set(
                    reviewRef,
                    reviewData,
                    SetOptions.merge(),
                )

                val productData = linkedMapOf<String, Any>(
                    "averageRating" to nextAverage,
                    "ratingCount" to nextCount,
                    "ratingTotal" to nextTotal,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                transaction.update(
                    productRef,
                    productData,
                )
            }
            null
        }.addOnSuccessListener {
            onResult(Result.success(Unit))
        }.addOnFailureListener { error ->
            onResult(Result.failure(error))
        }
    }

    private fun buildReviewId(orderId: String, productId: String, customerId: String): String {
        return listOf(orderId, productId, customerId)
            .joinToString("_")
            .replace(Regex("[^A-Za-z0-9_\\-]"), "_")
    }
}
