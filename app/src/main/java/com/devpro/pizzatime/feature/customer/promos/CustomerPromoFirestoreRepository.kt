package com.devpro.pizzatime.feature.customer.promos

import com.devpro.pizzatime.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object CustomerPromoFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadPromos(onResult: (Result<List<CustomerPromoUiModel>>) -> Unit) {
        firestore.collection("promoCodes")
            .get()
            .addOnSuccessListener { snapshot ->
                val promos = snapshot.documents
                    .sortedBy { it.getString("code") ?: "" }
                    .map { it.toCustomerPromoUiModel() }
                onResult(Result.success(promos))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun loadActivePromos(onResult: (Result<List<CustomerPromoUiModel>>) -> Unit) {
        loadPromos { result ->
            result.onSuccess { promos ->
                onResult(Result.success(promos.filter { it.state == CustomerPromoState.ACTIVE }))
            }.onFailure { error ->
                onResult(Result.failure(error))
            }
        }
    }

    private fun DocumentSnapshot.toCustomerPromoUiModel(): CustomerPromoUiModel {
        val discountType = getString("discountType") ?: "PERCENT"
        val discountValue = getDouble("discountValue") ?: 0.0
        val minOrderAmount = getDouble("minOrderAmount") ?: 0.0
        val state = resolveState()
        val statusLabel = when (state) {
            CustomerPromoState.ACTIVE -> "ACTIVE"
            CustomerPromoState.USED -> "USED"
            CustomerPromoState.EXPIRED -> "EXPIRED"
            CustomerPromoState.UNAVAILABLE -> "UNAVAILABLE"
        }
        return CustomerPromoUiModel(
            id = id,
            category = if (state == CustomerPromoState.ACTIVE) "DISCOUNT" else "PAST REWARD",
            code = getString("code") ?: id,
            description = getString("description") ?: "",
            metaLabel = if (minOrderAmount > 0) "MIN ORDER" else "DISCOUNT",
            metaValue = if (minOrderAmount > 0) {
                String.format(Locale.US, "$%.2f", minOrderAmount)
            } else {
                formatDiscount(discountType, discountValue)
            },
            statusLabel = statusLabel,
            actionLabel = if (state == CustomerPromoState.ACTIVE) "APPLY" else null,
            imageRes = R.drawable.img_pizza_time,
            state = state,
        )
    }

    private fun DocumentSnapshot.resolveState(): CustomerPromoState {
        val active = getBoolean("active") == true
        val used = getBoolean("used") == true ||
            getBoolean("redeemed") == true ||
            (getString("status")?.uppercase(Locale.US) == "USED")
        val expired = isExpired()
        return when {
            used -> CustomerPromoState.USED
            expired -> CustomerPromoState.EXPIRED
            active -> CustomerPromoState.ACTIVE
            else -> CustomerPromoState.UNAVAILABLE
        }
    }

    private fun DocumentSnapshot.isExpired(): Boolean {
        val timestamp = listOf(
            getTimestamp("expiresAt"),
            getTimestamp("endAt"),
            getTimestamp("validUntil"),
            getTimestamp("expiryAt"),
        ).firstOrNull()
        return timestamp?.toDate()?.time?.let { expiryTime ->
            expiryTime <= System.currentTimeMillis()
        } ?: false
    }

    private fun formatDiscount(type: String, value: Double): String {
        return when (type.uppercase(Locale.US)) {
            "PERCENT" -> "${value.toInt()}% Off"
            "FIXED" -> String.format(Locale.US, "$%.2f Off", value)
            else -> String.format(Locale.US, "%.0f Off", value)
        }
    }
}

