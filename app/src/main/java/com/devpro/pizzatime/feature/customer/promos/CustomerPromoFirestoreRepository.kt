package com.devpro.pizzatime.feature.customer.promos

import com.devpro.pizzatime.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object CustomerPromoFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadActivePromos(onResult: (Result<List<CustomerPromoUiModel>>) -> Unit) {
        firestore.collection("promoCodes")
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val promos = snapshot.documents
                    .sortedBy { it.getString("code") ?: "" }
                    .map { it.toCustomerPromoUiModel() }
                onResult(Result.success(promos))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    private fun DocumentSnapshot.toCustomerPromoUiModel(): CustomerPromoUiModel {
        val discountType = getString("discountType") ?: "PERCENT"
        val discountValue = getDouble("discountValue") ?: 0.0
        val minOrderAmount = getDouble("minOrderAmount") ?: 0.0
        return CustomerPromoUiModel(
            id = id,
            category = if (discountType.uppercase(Locale.US) == "PERCENT") "DISCOUNT" else "OFFER",
            code = getString("code") ?: id,
            description = getString("description") ?: "",
            metaLabel = if (minOrderAmount > 0) "MIN ORDER" else "DISCOUNT",
            metaValue = if (minOrderAmount > 0) {
                String.format(Locale.US, "$%.2f", minOrderAmount)
            } else {
                formatDiscount(discountType, discountValue)
            },
            statusLabel = "ACTIVE",
            actionLabel = "APPLY",
            imageRes = R.drawable.img_pizza_time,
            state = CustomerPromoState.ACTIVE,
        )
    }

    private fun formatDiscount(type: String, value: Double): String {
        return when (type.uppercase(Locale.US)) {
            "PERCENT" -> "${value.toInt()}% Off"
            "FIXED" -> String.format(Locale.US, "$%.2f Off", value)
            else -> String.format(Locale.US, "%.0f Off", value)
        }
    }
}

