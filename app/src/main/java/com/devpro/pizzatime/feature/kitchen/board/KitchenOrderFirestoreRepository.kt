package com.devpro.pizzatime.feature.kitchen.board

import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.kitchen.detail.KitchenOrderDetailItemUiModel
import com.devpro.pizzatime.feature.kitchen.detail.KitchenOrderDetailStatus
import com.devpro.pizzatime.feature.kitchen.detail.KitchenOrderDetailUiModel
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
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
    private val kitchenStatuses = listOf("CONFIRMED", "PREPARING", "BAKING", "READY", "READY_FOR_DELIVERY")

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

    fun loadOrderDetail(
        orderId: String,
        onResult: (Result<KitchenOrderDetailUiModel>) -> Unit,
    ) {
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onResult(Result.failure(Exception("Order $orderId not found.")))
                    return@addOnSuccessListener
                }
                onResult(Result.success(document.toKitchenOrderDetailUiModel()))
            }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
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

    fun cancelOrder(
        orderId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        OrderTransitionRepository.cancelByKitchen(
            orderId = orderId,
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
        val progressPercent = resolveKitchenProgress(statusStr)

        return KitchenOrderUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            fulfillmentLabel = mapFulfillmentLabel(orderType),
            timeLabel = createdAt.toTimeLabel(),
            status = kitchenStatus,
            items = rawItems?.mapNotNull { it.toKitchenItemUiModel() } ?: emptyList(),
            note = if (kitchenStatus == KitchenOrderStatus.READY) buildReadyNote(rawItems) else null,
            progressLabel = "$progressPercent% COMPLETE",
            progressPercent = progressPercent,
        )
    }

    private fun Any?.toKitchenItemUiModel(): KitchenOrderItemUiModel? {
        val map = this as? Map<*, *> ?: return null
        val name = map["name"] as? String ?: map["productName"] as? String ?: return null
        val quantity = map["quantity"].numberValue().toInt().coerceAtLeast(1)
        return KitchenOrderItemUiModel(
            quantity = quantity,
            name = name,
            sizeLabel = (map["selectedSize"] as? String).orEmpty(),
            modifier = (map["selectedToppings"] as? List<*>)
                ?.mapNotNull { it as? String }
                ?.joinToString()
                .orEmpty()
                .ifBlank { null },
            crust = (map["selectedCrust"] as? String).orEmpty().ifBlank { null },
        )
    }

    private fun DocumentSnapshot.toKitchenOrderDetailUiModel(): KitchenOrderDetailUiModel {
        val statusStr = getString("status") ?: "CONFIRMED"
        val rawItems = get("items") as? List<*>
        val allergyText = firstNotBlankString("allergy", "allergies", "allergyInfo", "allergenInfo")
        val request = firstNotBlankString("customerRequest", "specialRequest", "request", "note")

        return KitchenOrderDetailUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            receivedAgo = getTimestamp("receivedAt").toTimeLabel().ifBlank {
                getTimestamp("createdAt").toTimeLabel()
            },
            status = mapToKitchenDetailStatus(statusStr),
            item = buildDetailItem(rawItems),
            allergyTitle = allergyText?.let { "ALLERGY" },
            allergyMessage = allergyText,
            customerRequest = request ?: NO_VALUE,
            tags = buildKitchenTags(statusStr),
        )
    }

    private fun DocumentSnapshot.buildDetailItem(rawItems: List<*>?): KitchenOrderDetailItemUiModel {
        val itemMaps = rawItems?.mapNotNull { it as? Map<*, *> }.orEmpty()
        val names = itemMaps.mapNotNull { item ->
            val name = item["name"] as? String ?: item["productName"] as? String
            val quantity = item["quantity"].numberValue().toInt().coerceAtLeast(1)
            name?.let { "${quantity}x $it" }
        }
        val toppings = itemMaps.flatMap { item ->
            (item["selectedToppings"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        }.ifEmpty { listOf(NO_VALUE) }

        return KitchenOrderDetailItemUiModel(
            name = names.ifEmpty { listOf(displayOrderCode()) }.joinToString("\n"),
            size = itemMaps.mapNotNull { it["selectedSize"] as? String }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString()
                .ifBlank { NO_VALUE },
            crust = itemMaps.mapNotNull { it["selectedCrust"] as? String }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString()
                .ifBlank { NO_VALUE },
            toppings = toppings.distinct().take(5),
            imageRes = R.drawable.img_pizza_time,
        )
    }

    private fun mapToKitchenStatus(statusStr: String): KitchenOrderStatus {
        return when (statusStr.uppercase(Locale.US)) {
            "CONFIRMED" -> KitchenOrderStatus.WAITING
            "PREPARING", "BAKING" -> KitchenOrderStatus.PREPARING
            "READY", "READY_FOR_DELIVERY" -> KitchenOrderStatus.READY
            else -> KitchenOrderStatus.WAITING
        }
    }

    private fun mapToKitchenDetailStatus(statusStr: String): KitchenOrderDetailStatus {
        return when (statusStr.uppercase(Locale.US)) {
            "CONFIRMED" -> KitchenOrderDetailStatus.PENDING
            "PREPARING" -> KitchenOrderDetailStatus.PREPARING
            "BAKING" -> KitchenOrderDetailStatus.BAKING
            "READY", "READY_FOR_DELIVERY" -> KitchenOrderDetailStatus.READY
            "CANCELLED", "CANCELED" -> KitchenOrderDetailStatus.CANCELLED
            else -> KitchenOrderDetailStatus.PENDING
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

    private fun DocumentSnapshot.resolveKitchenProgress(statusStr: String): Int {
        val storedProgress = getLong("kitchenProgressPercent")?.toInt()
        if (storedProgress != null) return storedProgress.coerceIn(0, 100)
        return when (statusStr.uppercase(Locale.US)) {
            "CONFIRMED" -> 20
            "PREPARING" -> 40
            "BAKING" -> 75
            "READY", "READY_FOR_DELIVERY", "ASSIGNED_TO_SHIPPER", "DELIVERING", "DELIVERED" -> 100
            else -> 20
        }
    }

    private fun DocumentSnapshot.firstNotBlankString(vararg fields: String): String? {
        return fields.firstNotNullOfOrNull { field ->
            getString(field)?.trim()?.takeIf { it.isNotBlank() }
        }
    }

    private fun buildKitchenTags(statusStr: String): List<String> {
        return when (statusStr.uppercase(Locale.US)) {
            "BAKING" -> listOf("BAKING")
            "READY", "READY_FOR_DELIVERY" -> listOf("READY")
            else -> emptyList()
        }
    }

    private fun DocumentSnapshot.displayOrderCode(): String {
        return OrderCodeGenerator.displayOrderCode(
            orderCode = getString("orderCode"),
            orderId = id,
        )
    }

    private fun Timestamp?.toTimeLabel(): String {
        if (this == null) return ""
        val diffMins = TimeUnit.MILLISECONDS.toMinutes(
            System.currentTimeMillis() - seconds * 1000,
        ).coerceAtLeast(0)
        return "${diffMins}m"
    }

    private fun Any?.numberValue(): Double {
        return when (this) {
            is Number -> toDouble()
            else -> 0.0
        }
    }

    private const val NO_VALUE = "Không có"
}
