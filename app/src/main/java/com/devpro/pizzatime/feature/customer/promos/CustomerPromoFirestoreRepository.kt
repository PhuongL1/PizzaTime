package com.devpro.pizzatime.feature.customer.promos

import android.util.Log
import com.devpro.pizzatime.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private fun DocumentSnapshot.toCustomerPromoUiModel(): CustomerPromoUiModel {
        val code = firstString("code", "promoCode")
            .ifBlank { id.trim() }
            .uppercase(Locale.US)
        val title = firstString("title", "name")
        val description = firstString("description", "details")
            .ifBlank { title }
        val discountType = resolveDiscountType()
        val discountValue = resolveDiscountValue(discountType)
        val minOrderAmount = firstDouble("minOrderAmount", "minSubtotal", "minSpend", "minimumOrderAmount") ?: 0.0
        val state = resolveState()
        val statusText = when (state) {
            CustomerPromoState.ACTIVE -> "ACTIVE"
            CustomerPromoState.USED -> "USED"
            CustomerPromoState.EXPIRED -> "EXPIRED"
            CustomerPromoState.UNAVAILABLE -> "UNAVAILABLE"
        }
        val expiresAtMillis = firstDateMillis("expiresAt", "endAt", "validUntil", "expiryAt", "endDate")
        val statusLabel = when (state) {
            CustomerPromoState.ACTIVE -> statusText
            CustomerPromoState.USED -> statusText
            CustomerPromoState.EXPIRED -> statusText
            CustomerPromoState.UNAVAILABLE -> statusText
        }
        return CustomerPromoUiModel(
            id = id,
            category = if (state == CustomerPromoState.ACTIVE) "PROMO" else "PAST REWARD",
            code = code,
            description = description,
            metaLabel = when {
                expiresAtMillis != null -> "VALID UNTIL"
                minOrderAmount > 0 -> "MIN ORDER"
                else -> "DISCOUNT"
            },
            metaValue = when {
                expiresAtMillis != null -> formatDate(expiresAtMillis)
                minOrderAmount > 0 -> {
                String.format(Locale.US, "$%.2f", minOrderAmount)
                }
                else -> {
                formatDiscount(discountType, discountValue)
                }
            },
            statusLabel = statusLabel,
            actionLabel = if (state == CustomerPromoState.ACTIVE) "APPLY" else null,
            imageRes = R.drawable.img_pizza_time,
            state = state,
        )
    }

    private fun DocumentSnapshot.resolveState(): CustomerPromoState {
        val status = firstString("status").uppercase(Locale.US)
        val active = when (firstBoolean("active", "isActive", "enabled")) {
            true -> true
            false -> false
            null -> status !in setOf("INACTIVE", "DISABLED", "EXPIRED", "UNAVAILABLE")
        }
        val used = firstBoolean("used", "redeemed", "claimed") == true ||
            status == "USED"
        val expired = isExpired()
        val maxUses = firstLong("maxUses", "usageLimit", "maxUsage")
        val usedCount = firstLong("usedCount", "usageCount", "redeemedCount", "redemptionCount") ?: 0L
        val exhausted = maxUses != null && maxUses > 0 && usedCount >= maxUses
        return when {
            used -> CustomerPromoState.USED
            expired -> CustomerPromoState.EXPIRED
            exhausted -> CustomerPromoState.UNAVAILABLE
            active -> CustomerPromoState.ACTIVE
            else -> CustomerPromoState.UNAVAILABLE
        }
    }

    private fun DocumentSnapshot.isExpired(): Boolean {
        val expiryTime = firstDateMillis("expiresAt", "endAt", "validUntil", "expiryAt", "endDate")
        return expiryTime?.let { it <= System.currentTimeMillis() } ?: false
    }

    private fun DocumentSnapshot.resolveDiscountType(): String {
        val explicitType = firstString("discountType", "type")
        if (explicitType.isNotBlank()) {
            return explicitType.uppercase(Locale.US)
        }
        return when {
            firstDouble("discountPercent", "percentOff", "percentage") != null -> "PERCENT"
            firstDouble("discountAmount", "amountOff", "fixedAmount") != null -> "FIXED"
            else -> "PERCENT"
        }
    }

    private fun DocumentSnapshot.resolveDiscountValue(discountType: String): Double {
        return when (discountType.uppercase(Locale.US)) {
            "PERCENT" -> firstDouble("discountValue", "discountPercent", "percentOff", "percentage") ?: 0.0
            "FIXED" -> firstDouble("discountValue", "discountAmount", "amountOff", "fixedAmount") ?: 0.0
            else -> firstDouble("discountValue", "discountAmount", "discountPercent") ?: 0.0
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

    private fun DocumentSnapshot.firstString(vararg fieldNames: String): String {
        fieldNames.forEach { fieldName ->
            val value = getString(fieldName)
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    private fun DocumentSnapshot.firstBoolean(vararg fieldNames: String): Boolean? {
        fieldNames.forEach { fieldName ->
            val value = get(fieldName)
            val booleanValue = when (value) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                is Number -> value.toInt() != 0
                else -> null
            }
            if (booleanValue != null) {
                return booleanValue
            }
        }
        return null
    }

    private fun DocumentSnapshot.firstDouble(vararg fieldNames: String): Double? {
        fieldNames.forEach { fieldName ->
            val value = get(fieldName)
            val doubleValue = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
            if (doubleValue != null) {
                return doubleValue
            }
        }
        return null
    }

    private fun DocumentSnapshot.firstLong(vararg fieldNames: String): Long? {
        fieldNames.forEach { fieldName ->
            val value = get(fieldName)
            val longValue = when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
            if (longValue != null) {
                return longValue
            }
        }
        return null
    }

    private fun DocumentSnapshot.firstDateMillis(vararg fieldNames: String): Long? {
        fieldNames.forEach { fieldName ->
            val value = get(fieldName)
            val timeMillis = when (value) {
                is Timestamp -> value.toDate().time
                is Date -> value.time
                is Number -> value.toLong()
                is String -> value.toDateMillis()
                else -> null
            }
            if (timeMillis != null) {
                return timeMillis
            }
        }
        return null
    }

    private fun String.toDateMillis(): Long? {
        val value = trim()
        if (value.isBlank()) {
            return null
        }
        value.toLongOrNull()?.let { return it }
        DATE_PATTERNS.forEach { pattern ->
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }
                val parsed = parser.parse(value)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: ParseException) {
            }
        }
        return null
    }

    private val DATE_PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "MM/dd/yyyy",
        "dd/MM/yyyy",
    )

    private const val TAG = "CustomerPromoRepo"
}

