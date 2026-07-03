package com.devpro.pizzatime.feature.kitchen.board

import com.devpro.pizzatime.feature.order.OrderTransitionRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale
import java.util.concurrent.TimeUnit

object KitchenOrderFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val kitchenStatuses = listOf("CONFIRMED", "PREPARING", "BAKING")

    fun loadOrders(onResult: (Result<List<KitchenOrderUiModel>>) -> Unit) {
        firestore.collection("orders")
            .whereIn("status", kitchenStatuses)
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents.mapNotNull { it.toKitchenOrderUiModel() }
                onResult(Result.success(orders))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun listenOrders(onResult: (Result<List<KitchenOrderUiModel>>) -> Unit): ListenerRegistration {
        return firestore.collection("orders")
            .whereIn("status", kitchenStatuses)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents
                    ?.mapNotNull { it.toKitchenOrderUiModel() }
                    ?: emptyList()
                onResult(Result.success(orders))
            }
    }

    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        OrderTransitionRepository.updateByKitchen(
            orderId = orderId,
            newStatus = newStatus,
            kitchenId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            onResult = onResult,
        )
    }

    private fun DocumentSnapshot.toKitchenOrderUiModel(): KitchenOrderUiModel {
        val statusStr = getString("status") ?: "CONFIRMED"
        val kitchenStatus = mapToKitchenStatus(statusStr)
        val orderType = getString("orderType") ?: "DELIVERY"
        val createdAt = getTimestamp("createdAt")
        val rawItems = get("items") as? List<*>

        return KitchenOrderUiModel(
            orderId = id,
            fulfillmentLabel = mapFulfillmentLabel(orderType),
            timeLabel = createdAt.toTimeLabel(),
            status = kitchenStatus,
            items = rawItems?.mapNotNull { it.toKitchenItemUiModel() } ?: emptyList(),
            note = if (kitchenStatus == KitchenOrderStatus.READY) buildReadyNote(rawItems) else null,
        )
    }

    private fun Any?.toKitchenItemUiModel(): KitchenOrderItemUiModel? {
        val map = this as? Map<*, *> ?: return null
        val name = map["name"] as? String ?: return null
        val quantity = (map["quantity"] as? Long)?.toInt() ?: 1
        return KitchenOrderItemUiModel(
            quantity = quantity,
            name = name,
            sizeLabel = "",
            modifier = null,
            crust = null,
        )
    }

    private fun mapToKitchenStatus(statusStr: String): KitchenOrderStatus {
        return when (statusStr.uppercase(Locale.US)) {
            "CONFIRMED" -> KitchenOrderStatus.WAITING
            "PREPARING" -> KitchenOrderStatus.PREPARING
            "BAKING" -> KitchenOrderStatus.READY
            else -> KitchenOrderStatus.WAITING
        }
    }

    private fun mapFulfillmentLabel(orderType: String): String {
        return when (orderType.uppercase(Locale.US)) {
            "DELIVERY" -> "DELIVERY"
            "COLLECTION", "SELF_COLLECT" -> "COLLECTION"
            "DINE_IN" -> "DINE-IN"
            else -> orderType
        }
    }

    private fun buildReadyNote(rawItems: List<*>?): String {
        val count = rawItems?.size ?: 0
        return "$count Items Ready"
    }

    private fun Timestamp?.toTimeLabel(): String {
        if (this == null) return ""
        val diffMins = TimeUnit.MILLISECONDS.toMinutes(
            System.currentTimeMillis() - seconds * 1000,
        )
        return "${diffMins}m"
    }

}

