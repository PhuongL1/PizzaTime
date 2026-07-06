package com.devpro.pizzatime.feature.customer.checkout

import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.math.abs

object CheckoutConsistencyRepository {

    private const val PRICE_EPSILON = 0.005

    private val firestore = FirebaseFirestore.getInstance()

    fun validateCheckout(
        items: List<CartItemUiModel>,
        promoCode: String,
        onResult: (Result<CheckoutConsistencyResult>) -> Unit,
    ) {
        if (items.isEmpty()) {
            onResult(Result.success(CheckoutConsistencyResult.Valid(items, promoCode, 0.0)))
            return
        }

        val productRefs = items.map { item ->
            firestore.collection("products").document(item.id)
        }

        val productTasks = productRefs.map { it.get() }
        Tasks.whenAllSuccess<DocumentSnapshot>(productTasks)
            .addOnSuccessListener { productDocs ->
                val productResult = validateProducts(items, productDocs)
                if (productResult !is ProductValidationResult.Valid) {
                    onResult(Result.success(productResult.toCheckoutResult()))
                    return@addOnSuccessListener
                }

                validatePromoCode(
                    promoCode = promoCode,
                    subtotal = productResult.items.sumOf { it.price * it.quantity },
                    onResult = { promoResult ->
                        promoResult
                            .onSuccess { promoValidation ->
                                when (promoValidation) {
                                    is PromoValidationResult.Valid -> {
                                        onResult(
                                            Result.success(
                                                CheckoutConsistencyResult.Valid(
                                                    items = productResult.items,
                                                    promoCode = promoValidation.promoCode,
                                                    discount = promoValidation.discount,
                                                ),
                                            ),
                                        )
                                    }

                                    is PromoValidationResult.Invalid -> {
                                        onResult(Result.success(CheckoutConsistencyResult.PromoInvalid))
                                    }
                                }
                            }
                            .onFailure { error ->
                                onResult(Result.failure(error))
                            }
                    },
                )
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun validateProducts(
        cartItems: List<CartItemUiModel>,
        productDocs: List<DocumentSnapshot>,
    ): ProductValidationResult {
        val docsById = productDocs.associateBy { it.id }
        var priceChanged = false

        val latestItems = cartItems.map { item ->
            val doc = docsById[item.id]
            if (doc == null || !doc.exists() || doc.getBoolean("available") != true) {
                return ProductValidationResult.Unavailable
            }

            val latestPrice = doc.getDouble("basePrice") ?: return ProductValidationResult.Unavailable
            if (abs(latestPrice - item.price) > PRICE_EPSILON) {
                priceChanged = true
            }

            item.copy(
                name = doc.getString("name") ?: item.name,
                price = latestPrice,
            )
        }

        return if (priceChanged) {
            ProductValidationResult.PriceChanged(latestItems)
        } else {
            ProductValidationResult.Valid(latestItems)
        }
    }

    fun validatePromoCode(
        promoCode: String,
        subtotal: Double,
        onResult: (Result<PromoValidationResult>) -> Unit,
    ) {
        val normalizedCode = promoCode.trim().uppercase(Locale.US)
        if (normalizedCode.isBlank()) {
            onResult(Result.success(PromoValidationResult.Valid("", 0.0)))
            return
        }

        firestore.collection("promoCodes").document(normalizedCode)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists() || doc.isUnavailable()) {
                    onResult(Result.success(PromoValidationResult.Invalid(PromoValidationFailureReason.UNAVAILABLE)))
                    return@addOnSuccessListener
                }

                val minOrderAmount = doc.getDouble("minOrderAmount") ?: 0.0
                if (subtotal < minOrderAmount) {
                    onResult(Result.success(PromoValidationResult.Invalid(PromoValidationFailureReason.NOT_ELIGIBLE)))
                    return@addOnSuccessListener
                }

                val discountType = doc.getString("discountType") ?: "PERCENT"
                val discountValue = doc.getDouble("discountValue") ?: 0.0
                val discount = when (discountType.uppercase(Locale.US)) {
                    "PERCENT" -> (subtotal * discountValue / 100).coerceAtMost(subtotal)
                    "FIXED" -> discountValue.coerceAtMost(subtotal)
                    else -> 0.0
                }

                if (discount <= 0.0) {
                    onResult(Result.success(PromoValidationResult.Invalid(PromoValidationFailureReason.UNAVAILABLE)))
                    return@addOnSuccessListener
                }

                onResult(Result.success(PromoValidationResult.Valid(normalizedCode, discount)))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun ProductValidationResult.toCheckoutResult(): CheckoutConsistencyResult {
        return when (this) {
            ProductValidationResult.Unavailable -> CheckoutConsistencyResult.ItemsUnavailable
            is ProductValidationResult.PriceChanged -> CheckoutConsistencyResult.PriceChanged(items)
            is ProductValidationResult.Valid -> CheckoutConsistencyResult.Valid(items, "", 0.0)
        }
    }

    private fun DocumentSnapshot.isUnavailable(): Boolean {
        if (getBoolean("active") != true) {
            return true
        }
        if (isExpired()) {
            return true
        }
        val maxUses = getLong("maxUses") ?: getLong("usageLimit")
        if (maxUses != null) {
            val usedCount = getLong("usedCount") ?: getLong("usageCount") ?: getLong("redeemedCount") ?: 0L
            if (usedCount >= maxUses) {
                return true
            }
        }
        return false
    }

    private fun DocumentSnapshot.isExpired(): Boolean {
        val expiryTimestamp = listOfNotNull(
            getTimestamp("expiresAt"),
            getTimestamp("endAt"),
            getTimestamp("validUntil"),
            getTimestamp("expiryAt"),
        ).firstOrNull()
        return expiryTimestamp?.toDate()?.time?.let { expiryMillis ->
            expiryMillis <= System.currentTimeMillis()
        } ?: false
    }

    private sealed class ProductValidationResult {
        data class Valid(val items: List<CartItemUiModel>) : ProductValidationResult()
        data class PriceChanged(val items: List<CartItemUiModel>) : ProductValidationResult()
        data object Unavailable : ProductValidationResult()
    }

    sealed class PromoValidationResult {
        data class Valid(val promoCode: String, val discount: Double) : PromoValidationResult()
        data class Invalid(
            val reason: PromoValidationFailureReason = PromoValidationFailureReason.UNAVAILABLE,
        ) : PromoValidationResult()
    }

    enum class PromoValidationFailureReason {
        UNAVAILABLE,
        NOT_ELIGIBLE,
    }
}

sealed class CheckoutConsistencyResult {
    data class Valid(
        val items: List<CartItemUiModel>,
        val promoCode: String,
        val discount: Double,
    ) : CheckoutConsistencyResult()

    data class PriceChanged(val items: List<CartItemUiModel>) : CheckoutConsistencyResult()
    data object ItemsUnavailable : CheckoutConsistencyResult()
    data object PromoInvalid : CheckoutConsistencyResult()
}
