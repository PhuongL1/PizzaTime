package com.devpro.pizzatime.feature.admin.promo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object AdminPromoFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadPromos(onResult: (Result<List<AdminPromoUiModel>>) -> Unit) {
        firestore.collection("promoCodes")
            .get()
            .addOnSuccessListener { snapshot ->
                val promos = snapshot.documents
                    .sortedBy { it.getString("code") ?: "" }
                    .map { it.toAdminPromoUiModel() }
                onResult(Result.success(promos))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun setActive(promoId: String, active: Boolean, onResult: (Result<Unit>) -> Unit) {
        firestore.collection("promoCodes").document(promoId)
            .update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun updatePromo(
        promoId: String,
        title: String,
        description: String,
        discountType: String,
        discountValue: Double,
        minOrderAmount: Double,
        active: Boolean,
        onResult: (Result<Unit>) -> Unit,
    ) {
        firestore.collection("promoCodes").document(promoId)
            .update(
                mapOf(
                    "title" to title,
                    "description" to description,
                    "discountType" to discountType,
                    "discountValue" to discountValue,
                    "minOrderAmount" to minOrderAmount,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun createPromo(
        code: String,
        title: String,
        description: String,
        discountType: String,
        discountValue: Double,
        minOrderAmount: Double,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val promoRef = firestore.collection("promoCodes").document(code)
        promoRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onResult(Result.failure(Exception("Promo code already exists.")))
                    return@addOnSuccessListener
                }

                promoRef.set(
                    mapOf(
                        "code" to code,
                        "title" to title,
                        "description" to description,
                        "discountType" to discountType,
                        "discountValue" to discountValue,
                        "minOrderAmount" to minOrderAmount,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                    .addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { e -> onResult(Result.failure(e)) }
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    private fun DocumentSnapshot.toAdminPromoUiModel(): AdminPromoUiModel {
        val active = getBoolean("active") ?: true
        val discountType = getString("discountType") ?: "PERCENT"
        val discountValue = getDouble("discountValue") ?: 0.0
        val minOrderAmount = getDouble("minOrderAmount") ?: 0.0
        return AdminPromoUiModel(
            id = id,
            code = getString("code") ?: id,
            title = getString("title") ?: "",
            description = getString("description") ?: "",
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            status = if (active) AdminPromoStatus.ACTIVE else AdminPromoStatus.INACTIVE,
            discountText = formatDiscount(discountType, discountValue),
            minSpendText = if (minOrderAmount > 0) String.format(Locale.US, "$%.2f", minOrderAmount) else null,
            isHighlighted = active,
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

