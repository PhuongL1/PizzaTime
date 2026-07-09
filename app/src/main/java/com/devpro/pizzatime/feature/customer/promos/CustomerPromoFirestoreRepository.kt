package com.devpro.pizzatime.feature.customer.promos

import android.util.Log
import com.devpro.pizzatime.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.devpro.pizzatime.shared.promo.toPromoDocumentModel

object CustomerPromoFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadPromos(onResult: (Result<List<CustomerPromoUiModel>>) -> Unit) {
        firestore.collection("promoCodes")
            .get()
            .addOnSuccessListener { snapshot ->
                val promos = snapshot.documents
                    .mapNotNull { doc ->
                        runCatching { doc.toCustomerPromoUiModel() }
                            .onFailure { error ->
                                Log.e(TAG, "Promo mapping failed for id=${doc.id}", error)
                            }
                            .getOrNull()
                    }
                    .sortedBy { promo -> promo.code }
                Log.d(TAG, "customer loaded count=${promos.size}")
                onResult(Result.success(promos))
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Promo load failed", error)
                onResult(Result.failure(error))
            }
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

    private fun com.google.firebase.firestore.DocumentSnapshot.toCustomerPromoUiModel(): CustomerPromoUiModel {
        val promo = toPromoDocumentModel()
        val state = resolveState(promo)
        val statusText = when (state) {
            CustomerPromoState.ACTIVE -> "ACTIVE"
            CustomerPromoState.USED -> "USED"
            CustomerPromoState.EXPIRED -> "EXPIRED"
            CustomerPromoState.UNAVAILABLE -> "UNAVAILABLE"
        }
        val statusLabel = when (state) {
            CustomerPromoState.ACTIVE -> if (promo.expiresAtMillis != null) "AVAILABLE" else statusText
            CustomerPromoState.USED -> statusText
            CustomerPromoState.EXPIRED -> statusText
            CustomerPromoState.UNAVAILABLE -> if (promo.isScheduled) "UPCOMING" else statusText
        }
        return CustomerPromoUiModel(
            id = promo.id,
            category = if (state == CustomerPromoState.ACTIVE) "PROMO" else "PAST REWARD",
            code = promo.code,
            description = promo.description,
            metaLabel = when {
                promo.expiresAtMillis != null -> "VALID UNTIL"
                promo.startsAtMillis != null && !promo.isStarted -> "STARTS"
                promo.minOrderAmount > 0 -> "MIN ORDER"
                else -> "DISCOUNT"
            },
            metaValue = when {
                promo.expiresAtMillis != null -> formatDate(promo.expiresAtMillis)
                promo.startsAtMillis != null && !promo.isStarted -> formatDate(promo.startsAtMillis)
                promo.minOrderAmount > 0 -> String.format(Locale.US, "$%.2f", promo.minOrderAmount)
                else -> formatDiscount(promo.discountType, promo.discountValue)
            },
            statusLabel = statusLabel,
            actionLabel = if (state == CustomerPromoState.ACTIVE) "APPLY" else null,
            imageRes = R.drawable.img_pizza_time,
            state = state,
        )
    }

    private fun resolveState(promo: com.devpro.pizzatime.shared.promo.PromoDocumentModel): CustomerPromoState {
        return when {
            promo.usedFlag -> CustomerPromoState.USED
            promo.isExpired -> CustomerPromoState.EXPIRED
            promo.isAvailableForCustomer -> CustomerPromoState.ACTIVE
            else -> CustomerPromoState.UNAVAILABLE
        }
    }

    private fun formatDiscount(type: String, value: Double): String {
        return when (type.uppercase(Locale.US)) {
            "PERCENT" -> "${value.toInt()}% Off"
            "FIXED" -> String.format(Locale.US, "$%.2f Off", value)
            else -> String.format(Locale.US, "%.0f Off", value)
        }
    }

    private fun formatDate(timeMillis: Long): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timeMillis))
    }

    private const val TAG = "CustomerPromoRepo"
}

