package com.devpro.pizzatime.feature.customer.checkout

import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.admin.store.StoreSettingsUiModel
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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
        deliveryLat: Double?,
        deliveryLng: Double?,
        customerName: String = "",
        customerPhone: String = "",
        storeSettings: StoreSettingsUiModel,
        promoCode: String = "",
        onResult: (Result<String>) -> Unit,
    ) {
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
            "paymentMethod" to "CASH_ON_DELIVERY",
            "paymentStatus" to "UNPAID",
            "cashCollected" to false,
            "deliveryAddress" to deliveryAddress,
            "deliveryLat" to deliveryLat,
            "deliveryLng" to deliveryLng,
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

    private class OrderCodeCollisionException : Exception()
}

