package com.devpro.pizzatime.feature.customer.checkout

import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.admin.store.StoreSettingsUiModel
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.devpro.pizzatime.feature.order.OrderPaymentHandoffParser
import com.devpro.pizzatime.feature.order.PaymentMethod
import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import com.devpro.pizzatime.shared.location.OrderDeliveryDestinationResolver
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs
import kotlin.math.roundToInt

object FirebaseOrderRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private const val MAX_ORDER_CODE_ATTEMPTS = 20
    private const val UNIQUE_ORDER_CODE_FAILURE =
        "Could not create a unique order code. Please try again."

    fun createOrder(
        customerId: String,
        customerEmail: String,
        items: List<CartItemUiModel>,
        distanceKm: Double,
        deliveryFee: Double,
        itemsSubtotal: Double,
        discountAmount: Double,
        finalTotal: Double,
        deliveryAddress: String = "",
        deliveryCoordinate: DeliveryCoordinate,
        customerName: String = "",
        customerPhone: String = "",
        storeSettings: StoreSettingsUiModel,
        paymentMethod: PaymentMethod = PaymentMethod.COD,
        promoCode: String = "",
        onResult: (Result<String>) -> Unit,
    ) {
        if (deliveryAddress.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("A delivery address is required.")))
            return
        }
        val orderItems = items.map { item ->
            hashMapOf(
                "productId" to item.id,
                "productName" to item.name,
                "name" to item.name,
                "quantity" to item.quantity,
                "unitPrice" to item.price,
                "totalPrice" to item.price * item.quantity,
                "imageUrl" to item.imageUrl,
                "selectedSize" to item.selectedSize,
                "selectedCrust" to item.selectedCrust,
                "selectedToppings" to item.selectedToppings,
            )
        }
        val trustedPricingSnapshot = runCatching {
            buildTrustedPricingSnapshot(
                itemsSubtotal = itemsSubtotal,
                discountAmount = discountAmount,
                deliveryFee = deliveryFee,
                finalTotal = finalTotal,
            )
        }.getOrElse { error ->
            onResult(Result.failure(error))
            return
        }

        val baseOrder = hashMapOf(
            "customerId" to customerId,
            "customerEmail" to customerEmail,
            "customerName" to customerName.ifBlank { customerEmail },
            "customerPhone" to customerPhone,
            "storeName" to storeSettings.storeName,
            "pickupAddress" to storeSettings.pickupAddress,
            "pickupLat" to storeSettings.pickupLat,
            "pickupLng" to storeSettings.pickupLng,
            "storePhone" to storeSettings.storePhone,
            "status" to "PENDING",
            "orderType" to "DELIVERY",
            "distanceKm" to distanceKm,
            "itemsSubtotal" to itemsSubtotal,
            "subtotal" to itemsSubtotal,
            "deliveryFee" to deliveryFee,
            "discountAmount" to discountAmount,
            "discount" to discountAmount,
            "promoCode" to promoCode,
            "finalTotal" to finalTotal,
            "total" to finalTotal,
            "note" to "",
            "items" to orderItems,
            "pricingSnapshotVnd" to trustedPricingSnapshot,
            "statusHistory" to listOf(
                buildHistoryItem(
                    status = "PENDING",
                    actorRole = "CUSTOMER",
                    actorId = customerId,
                    note = "Order placed",
                ),
            ),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        baseOrder.putAll(createPaymentFields(paymentMethod))
        if (paymentMethod == PaymentMethod.DEMO) {
            baseOrder[OrderPaymentHandoffParser.FIELD_PAYMENT_PROVIDER] = PaymentMethod.DEMO.name
        }
        baseOrder.putAll(
            OrderDeliveryDestinationResolver.canonicalFields(
                address = deliveryAddress,
                coordinate = deliveryCoordinate,
            ),
        )

        createOrderWithUniqueCode(
            baseOrder = baseOrder,
            attempt = 1,
            onResult = onResult,
        )
    }

    private fun createOrderWithUniqueCode(
        baseOrder: HashMap<String, Any?>,
        attempt: Int,
        onResult: (Result<String>) -> Unit,
    ) {
        if (attempt > MAX_ORDER_CODE_ATTEMPTS) {
            onResult(Result.failure(Exception(UNIQUE_ORDER_CODE_FAILURE)))
            return
        }

        val orderCodeKey = OrderCodeGenerator.generateOrderCodeKey()
        val orderDoc = firestore.collection("orders").document(orderCodeKey)
        val order = HashMap(baseOrder).apply {
            put("orderId", orderCodeKey)
            put("orderCodeKey", orderCodeKey)
            put("orderCode", OrderCodeGenerator.displayOrderCode(orderCodeKey))
        }

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(orderDoc)
            if (snapshot.exists()) {
                throw OrderCodeCollisionException()
            }
            transaction.set(orderDoc, order)
            orderCodeKey
        }
            .addOnSuccessListener { createdOrderId ->
                onResult(Result.success(createdOrderId))
            }
            .addOnFailureListener { error ->
                if (error is OrderCodeCollisionException) {
                    createOrderWithUniqueCode(
                        baseOrder = baseOrder,
                        attempt = attempt + 1,
                        onResult = onResult,
                    )
                } else {
                    onResult(Result.failure(Exception(error.message ?: "Failed to create order.")))
                }
            }
    }

    private fun buildHistoryItem(
        status: String,
        actorRole: String,
        actorId: String,
        note: String,
    ): HashMap<String, Any> {
        return hashMapOf(
            "status" to status,
            "actorRole" to actorRole,
            "actorId" to actorId,
            "note" to note,
            "createdAt" to Timestamp.now(),
        )
    }

    private fun createPaymentFields(paymentMethod: PaymentMethod): Map<String, Any> {
        return when (paymentMethod) {
            PaymentMethod.COD -> OrderPaymentHandoffParser.codCreateFields()
            PaymentMethod.DEMO -> OrderPaymentHandoffParser.futureDemoCreateFields()
            PaymentMethod.VNPAY -> OrderPaymentHandoffParser.futureVnpayCreateFields()
            PaymentMethod.UNKNOWN -> OrderPaymentHandoffParser.codCreateFields()
        }
    }

    private fun buildTrustedPricingSnapshot(
        itemsSubtotal: Double,
        discountAmount: Double,
        deliveryFee: Double,
        finalTotal: Double,
    ): Map<String, Any> {
        val itemsSubtotalVnd = toTrustedVnd(itemsSubtotal)
        val discountVnd = toTrustedVnd(discountAmount)
        val deliveryFeeVnd = toTrustedVnd(deliveryFee)
        val totalVnd = toTrustedVnd(finalTotal)
        if (itemsSubtotalVnd - discountVnd + deliveryFeeVnd != totalVnd) {
            throw IllegalArgumentException("Trusted pricing snapshot does not reconcile.")
        }
        return mapOf(
            "schemaVersion" to 1,
            "currency" to "VND",
            "itemsSubtotalVnd" to itemsSubtotalVnd,
            "discountVnd" to discountVnd,
            "deliveryFeeVnd" to deliveryFeeVnd,
            "totalVnd" to totalVnd,
        )
    }

    private fun toTrustedVnd(value: Double): Int {
        val rounded = value.roundToInt()
        if (abs(value - rounded) > TRUSTED_VND_EPSILON) {
            throw IllegalArgumentException("Checkout total is not an integer-VND value.")
        }
        return rounded
    }

    private class OrderCodeCollisionException : Exception()
    private const val TRUSTED_VND_EPSILON = 0.001
}

