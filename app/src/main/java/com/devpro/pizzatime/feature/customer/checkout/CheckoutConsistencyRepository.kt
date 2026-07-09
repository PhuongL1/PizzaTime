package com.devpro.pizzatime.feature.customer.checkout

import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.shared.promo.firstDouble
import com.devpro.pizzatime.shared.promo.firstString
import com.devpro.pizzatime.shared.promo.resolveDiscountType
import com.devpro.pizzatime.shared.promo.resolveDiscountValue
import com.devpro.pizzatime.shared.promo.toPromoDocumentModel
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
                if (doc.exists()) {
                    onResult(Result.success(doc.toPromoValidationResult(normalizedCode, subtotal)))
                    return@addOnSuccessListener
                }
                findPromoByCode(normalizedCode) { queryResult ->
                    queryResult
                        .onSuccess { matchedDoc ->
                            if (matchedDoc == null) {
                                onResult(
                                    Result.success(
                                        PromoValidationResult.Invalid(PromoValidationFailureReason.UNAVAILABLE),
                                    ),
                                )
                            } else {
                                onResult(Result.success(matchedDoc.toPromoValidationResult(normalizedCode, subtotal)))
                            }
                        }
                        .onFailure { error ->
                            onResult(Result.failure(error))
                        }
                }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun findPromoByCode(
        normalizedCode: String,
        onResult: (Result<DocumentSnapshot?>) -> Unit,
    ) {
        firestore.collection("promoCodes")
            .get()
            .addOnSuccessListener { snapshot ->
                val matchedDoc = snapshot.documents.firstOrNull { doc ->
                    doc.firstString("code", "promoCode")
                        .uppercase(Locale.US) == normalizedCode
                }
                onResult(Result.success(matchedDoc))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun DocumentSnapshot.toPromoValidationResult(
        normalizedCode: String,
        subtotal: Double,
    ): PromoValidationResult {
        if (!exists() || isUnavailable()) {
            return PromoValidationResult.Invalid(PromoValidationFailureReason.UNAVAILABLE)
        }

        val minOrderAmount = firstDouble("minOrderAmount", "minSubtotal", "minSpend", "minimumOrderAmount") ?: 0.0
        if (subtotal < minOrderAmount) {
            return PromoValidationResult.Invalid(PromoValidationFailureReason.NOT_ELIGIBLE)
        }

        val discountType = resolveDiscountType()
        val discountValue = resolveDiscountValue(discountType)
        val discount = when (discountType.uppercase(Locale.US)) {
            "PERCENT" -> (subtotal * discountValue / 100).coerceAtMost(subtotal)
            "FIXED" -> discountValue.coerceAtMost(subtotal)
            else -> 0.0
        }

        if (discount <= 0.0) {
            return PromoValidationResult.Invalid(PromoValidationFailureReason.UNAVAILABLE)
        }

        return PromoValidationResult.Valid(
            promoCode = firstString("code", "promoCode").ifBlank { normalizedCode },
            discount = discount,
        )
    }

    private fun ProductValidationResult.toCheckoutResult(): CheckoutConsistencyResult {
        return when (this) {
            ProductValidationResult.Unavailable -> CheckoutConsistencyResult.ItemsUnavailable
            is ProductValidationResult.PriceChanged -> CheckoutConsistencyResult.PriceChanged(items)
            is ProductValidationResult.Valid -> CheckoutConsistencyResult.Valid(items, "", 0.0)
        }
    }

    private fun DocumentSnapshot.isUnavailable(): Boolean {
        val promo = toPromoDocumentModel()
        return !promo.isAvailableForCustomer
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
