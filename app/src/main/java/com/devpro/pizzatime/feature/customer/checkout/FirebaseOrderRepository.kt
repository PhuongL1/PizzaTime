package com.devpro.pizzatime.feature.customer.checkout

import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseOrderRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun createOrder(
        customerId: String,
        customerEmail: String,
        items: List<CartItemUiModel>,
        deliveryFee: Double,
        promoCode: String = "",
        discount: Double = 0.0,
        onResult: (Result<String>) -> Unit,
    ) {
        val subtotal = items.sumOf { it.price * it.quantity }
        val total = subtotal - discount + deliveryFee

        val orderItems = items.map { item ->
            hashMapOf(
                "productId" to item.id,
                "name" to item.name,
                "quantity" to item.quantity,
                "unitPrice" to item.price,
                "totalPrice" to item.price * item.quantity,
                "imageUrl" to "",
            )
        }

        val order = hashMapOf(
            "customerId" to customerId,
            "customerEmail" to customerEmail,
            "customerName" to customerEmail,
            "status" to "PENDING",
            "orderType" to "DELIVERY",
            "paymentMethod" to "CASH",
            "deliveryAddress" to "",
            "subtotal" to subtotal,
            "deliveryFee" to deliveryFee,
            "discount" to discount,
            "promoCode" to promoCode,
            "total" to total,
            "note" to "",
            "items" to orderItems,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        firestore.collection("orders")
            .add(order)
            .addOnSuccessListener { docRef ->
                onResult(Result.success(docRef.id))
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(Exception(e.message ?: "Failed to create order.")))
            }
    }
}

