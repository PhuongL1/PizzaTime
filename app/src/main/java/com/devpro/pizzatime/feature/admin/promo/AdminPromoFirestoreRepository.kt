package com.devpro.pizzatime.feature.admin.promo

import android.util.Log
import com.devpro.pizzatime.shared.promo.toPromoDocumentModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AdminPromoFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadPromos(onResult: (Result<List<AdminPromoUiModel>>) -> Unit) {
        firestore.collection("promoCodes")
            .get()
            .addOnSuccessListener { snapshot ->
                val promos = snapshot.documents
                    .mapNotNull { doc ->
                        runCatching { doc.toAdminPromoUiModel() }
                            .onFailure { error ->
                                Log.e(TAG, "Admin promo mapping failed for id=${doc.id}", error)
                            }
                            .getOrNull()
                    }
                    .sortedBy { it.code }
                Log.d(TAG, "admin loaded count=${promos.size}")
                onResult(Result.success(promos))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Admin promo load failed", e)
                onResult(Result.failure(e))
            }
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
        val promo = toPromoDocumentModel()
        val status = resolveStatus(promo)
        return AdminPromoUiModel(
            id = promo.id,
            code = promo.code,
            title = promo.title,
            description = promo.description,
            discountType = promo.discountType,
            discountValue = promo.discountValue,
            minOrderAmount = promo.minOrderAmount,
            status = status,
            discountText = formatDiscount(promo.discountType, promo.discountValue),
            expiryText = promo.expiresAtMillis?.let(::formatDate),
            minSpendText = if (promo.minOrderAmount > 0) String.format(Locale.US, "$%.2f", promo.minOrderAmount) else null,
            endsInText = formatEndsInText(promo.startsAtMillis, promo.expiresAtMillis, status),
            usedText = formatUsedText(promo.usageCount, promo.maxUses),
            usageCount = promo.usageCount,
            maxUses = promo.maxUses,
            totalReach = promo.totalReach,
            isHighlighted = status == AdminPromoStatus.ACTIVE,
        )
    }

    private fun resolveStatus(promo: com.devpro.pizzatime.shared.promo.PromoDocumentModel): AdminPromoStatus {
        return when {
            promo.rawStatus == "EXPIRED" || promo.isExpired -> AdminPromoStatus.EXPIRED
            !promo.isStarted -> AdminPromoStatus.SCHEDULED
            !promo.activeFlag || promo.rawStatus in setOf("INACTIVE", "DISABLED", "UNAVAILABLE") -> {
                AdminPromoStatus.INACTIVE
            }
            promo.rawStatus == "SCHEDULED" && promo.isStarted -> AdminPromoStatus.ACTIVE
            promo.activeFlag -> AdminPromoStatus.ACTIVE
            else -> AdminPromoStatus.INACTIVE
        }
    }

    private fun formatEndsInText(
        startsAtMillis: Long?,
        expiresAtMillis: Long?,
        status: AdminPromoStatus,
    ): String? {
        val now = System.currentTimeMillis()
        return when {
            status == AdminPromoStatus.SCHEDULED && startsAtMillis != null && startsAtMillis > now ->
                "Starts ${formatDate(startsAtMillis)}"
            expiresAtMillis != null && expiresAtMillis > now ->
                "Ends ${formatDate(expiresAtMillis)}"
            else -> null
        }
    }

    private fun formatUsedText(usageCount: Int, maxUses: Int?): String {
        return if (maxUses != null && maxUses > 0) {
            "$usageCount/$maxUses used"
        } else {
            "$usageCount used"
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

    private const val TAG = "AdminPromoRepo"
}

