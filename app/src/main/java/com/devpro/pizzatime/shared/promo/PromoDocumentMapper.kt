package com.devpro.pizzatime.shared.promo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PromoDocumentModel(
    val id: String,
    val code: String,
    val title: String,
    val description: String,
    val discountType: String,
    val discountValue: Double,
    val minOrderAmount: Double,
    val usageCount: Int,
    val maxUses: Int?,
    val totalReach: Int?,
    val startsAtMillis: Long?,
    val expiresAtMillis: Long?,
    val rawStatus: String,
    val activeFlag: Boolean,
    val usedFlag: Boolean,
) {
    val isStarted: Boolean
        get() = startsAtMillis?.let { it <= System.currentTimeMillis() } ?: true

    val isExpired: Boolean
        get() = expiresAtMillis?.let { it <= System.currentTimeMillis() } ?: false

    val isExhausted: Boolean
        get() = maxUses != null && maxUses > 0 && usageCount >= maxUses

    val isAvailableForCustomer: Boolean
        get() = activeFlag && isStarted && !isExpired && !isExhausted && !usedFlag

    val isInactive: Boolean
        get() = !activeFlag || rawStatus in setOf("INACTIVE", "DISABLED")

    val isScheduled: Boolean
        get() = rawStatus == "SCHEDULED" || !isStarted
}

fun DocumentSnapshot.toPromoDocumentModel(): PromoDocumentModel {
    val rawStatus = firstString("status").uppercase(Locale.US)
    val code = firstString("code", "promoCode")
        .ifBlank { id.trim() }
        .uppercase(Locale.US)
    val title = firstString("title", "name")
        .ifBlank { code }
    val description = firstString("description", "details", "summary")
        .ifBlank { title }
    val discountType = resolveDiscountType()
    val usageCount = firstInt("usageCount", "usedCount", "redemptionCount", "redeemedCount", "redemptions") ?: 0
    val maxUses = firstInt("maxUses", "maxUsage", "usageLimit")
    val startsAtMillis = firstDateMillis("startsAt", "startAt", "startDate", "validFrom")
    val expiresAtMillis = firstDateMillis("expiresAt", "endAt", "validUntil", "expiryAt", "endDate")
    return PromoDocumentModel(
        id = id,
        code = code,
        title = title,
        description = description,
        discountType = discountType,
        discountValue = resolveDiscountValue(discountType),
        minOrderAmount = firstDouble("minOrderAmount", "minSubtotal", "minSpend", "minimumOrderAmount") ?: 0.0,
        usageCount = usageCount,
        maxUses = maxUses,
        totalReach = firstInt("totalReach", "reach", "eligibleUsers", "audience", "maxUses", "usageCount"),
        startsAtMillis = startsAtMillis,
        expiresAtMillis = expiresAtMillis,
        rawStatus = rawStatus,
        activeFlag = when (firstBoolean("active", "isActive", "enabled")) {
            true -> true
            false -> false
            null -> rawStatus !in setOf("INACTIVE", "DISABLED", "UNAVAILABLE")
        },
        usedFlag = firstBoolean("used", "redeemed", "claimed") == true || rawStatus == "USED",
    )
}

fun DocumentSnapshot.firstString(vararg fieldNames: String): String {
    fieldNames.forEach { fieldName ->
        val value = getString(fieldName)
        if (!value.isNullOrBlank()) {
            return value.trim()
        }
    }
    return ""
}

fun DocumentSnapshot.firstBoolean(vararg fieldNames: String): Boolean? {
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

fun DocumentSnapshot.firstDouble(vararg fieldNames: String): Double? {
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

fun DocumentSnapshot.firstInt(vararg fieldNames: String): Int? {
    fieldNames.forEach { fieldName ->
        val value = get(fieldName)
        val intValue = when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
        if (intValue != null) {
            return intValue.coerceAtLeast(0)
        }
    }
    return null
}

fun DocumentSnapshot.firstLong(vararg fieldNames: String): Long? {
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

fun DocumentSnapshot.firstDateMillis(vararg fieldNames: String): Long? {
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

fun DocumentSnapshot.resolveDiscountType(): String {
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

fun DocumentSnapshot.resolveDiscountValue(discountType: String): Double {
    return when (discountType.uppercase(Locale.US)) {
        "PERCENT" -> firstDouble("discountValue", "discountPercent", "percentOff", "percentage") ?: 0.0
        "FIXED" -> firstDouble("discountValue", "discountAmount", "amountOff", "fixedAmount") ?: 0.0
        else -> firstDouble("discountValue", "discountAmount", "discountPercent") ?: 0.0
    }
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
