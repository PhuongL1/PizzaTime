package com.devpro.pizzatime.feature.customer.orderhistory

import android.util.Log
import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.customer.orderdetail.CustomerBillUiModel
import com.devpro.pizzatime.feature.customer.orderdetail.CustomerOrderDetailUiModel
import com.devpro.pizzatime.feature.customer.orderdetail.CustomerOrderItemUiModel
import com.devpro.pizzatime.feature.customer.orderdetail.CustomerOrderStatusHistoryUiModel
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.devpro.pizzatime.feature.order.OrderTransitionRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CustomerOrderFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadOrderHistory(
        customerId: String,
        onResult: (Result<List<CustomerOrderHistoryItemUiModel>>) -> Unit,
    ) {
        firestore.collection("orders")
            .whereEqualTo("customerId", customerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents
                    .sortedByDescending { it.getTimestamp("createdAt")?.seconds ?: 0L }
                    .mapNotNull { it.toHistoryItem() }
                enrichHistoryImages(orders) { enrichedOrders ->
                    onResult(Result.success(enrichedOrders))
                }
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun loadOrderDetail(
        orderId: String,
        customerId: String,
        onResult: (Result<CustomerOrderDetailUiModel>) -> Unit,
    ) {
        if (customerId.isBlank()) {
            onResult(Result.failure(IllegalStateException("Authenticated customer required")))
            return
        }
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists() || doc.getString("customerId") != customerId) {
                    onResult(Result.failure(NoSuchElementException("Customer order unavailable")))
                    return@addOnSuccessListener
                }
                enrichOrderDetailImages(doc.toOrderDetail()) { detail ->
                    onResult(Result.success(detail))
                }
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun cancelOrder(
        orderId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(Result.failure(Exception("Please log in to cancel this order.")))
            return
        }

        OrderTransitionRepository.cancelByCustomer(
            orderId = orderId,
            customerId = uid,
            reason = null,
            onResult = onResult,
        )
    }

    private fun DocumentSnapshot.toHistoryItem(): CustomerOrderHistoryItemUiModel {
        val statusStr = getString("status") ?: "PENDING"
        val total = getDouble("finalTotal") ?: getDouble("total") ?: 0.0
        val createdAt = getTimestamp("createdAt")
        val rawItems = get("items") as? List<*>
        val items = rawItems?.mapNotNull { it.toOrderItem() }.orEmpty()
        val heroItem = items.maxByOrNull { it.price }

        return CustomerOrderHistoryItemUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            orderedAt = createdAt?.toHistoryDateString() ?: "",
            status = mapHistoryStatus(statusStr),
            itemSummary = buildItemSummary(rawItems),
            total = total,
            imageRes = if (heroItem != null) R.drawable.img_pizza_time else null,
            imageUrl = heroItem?.imageUrl.orEmpty(),
            heroProductId = heroItem?.productId.orEmpty(),
        )
    }

    private fun DocumentSnapshot.toOrderDetail(): CustomerOrderDetailUiModel {
        val statusStr = getString("status") ?: "PENDING"
        val total = getDouble("finalTotal") ?: getDouble("total") ?: 0.0
        val subtotal = getDouble("itemsSubtotal") ?: getDouble("subtotal") ?: 0.0
        val deliveryFee = getDouble("deliveryFee") ?: 0.0
        val discount = getDouble("discountAmount") ?: getDouble("discount") ?: 0.0
        val createdAt = getTimestamp("createdAt")
        val rawItems = get("items") as? List<*>
        val rawStatusHistory = get("statusHistory") as? List<*>
        val items = rawItems?.mapNotNull { it.toOrderItem() } ?: emptyList()
        val heroItem = items.maxByOrNull { it.price }

        return CustomerOrderDetailUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            statusLabel = statusStr,
            orderTime = createdAt?.toDisplayTime() ?: "",
            heroImageRes = heroItem?.imageRes ?: R.drawable.img_pizza_time,
            heroImageUrl = heroItem?.imageUrl.orEmpty(),
            heroMessage = mapStatusMessage(statusStr),
            items = items,
            bill = CustomerBillUiModel(
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                taxes = 0.0,
                discountLabel = if (discount > 0) "Discount" else "",
                discount = if (discount > 0) -discount else 0.0,
                total = total,
            ),
            deliveryAddressTitle = when (statusStr) {
                "DELIVERED" -> "DELIVERED TO"
                STATUS_CANCELLED -> "DELIVERY ADDRESS"
                else -> "DELIVERING TO"
            },
            deliveryAddressLine1 = getString("deliveryAddress").orNotProvided(),
            deliveryAddressLine2 = "",
            storeName = getString("storeName").orNotProvided(),
            pickupAddress = getString("pickupAddress").orNotProvided(),
            storePhone = getString("storePhone").orNotProvided(),
            distanceKm = getDouble("distanceKm"),
            paymentMethod = getString("paymentMethod").toPaymentMethodLabel(),
            paymentStatus = paymentStatusLabel(
                stored = getString("paymentStatus"),
                status = statusStr,
            ),
            statusHistory = rawStatusHistory.toStatusHistoryUiModels(),
            canCancel = statusStr == STATUS_PENDING,
        )
    }

    private fun Any?.toOrderItem(): CustomerOrderItemUiModel? {
        val map = this as? Map<*, *> ?: return null
        val name = map["name"] as? String ?: return null
        val quantity = map["quantity"].asIntOrNull()?.coerceAtLeast(1) ?: 1
        val linePrice = map["totalPrice"].asDoubleOrNull()
            ?: map["lineTotal"].asDoubleOrNull()
            ?: map["unitPrice"].asDoubleOrNull()?.times(quantity)
            ?: map["price"].asDoubleOrNull()
            ?: 0.0
        val productId = (map["productId"] as? String ?: map["id"] as? String).orEmpty().ifBlank {
            name.lowercase(Locale.US).replace(Regex("\\s+"), "_")
        }
        return CustomerOrderItemUiModel(
            productId = productId,
            quantity = quantity,
            name = name,
            description = buildItemDescription(map),
            price = linePrice,
            imageRes = null,
            imageUrl = map.firstString("imageUrl", "productImageUrl", "photoUrl"),
        )
    }

    private fun enrichOrderDetailImages(
        detail: CustomerOrderDetailUiModel,
        onResult: (CustomerOrderDetailUiModel) -> Unit,
    ) {
        val missingProductIds = detail.items
            .filter { item -> item.imageUrl.isBlank() }
            .map { item -> item.productId }
            .filter { productId -> productId.isNotBlank() }
            .toSet()

        loadProductImageUrls(missingProductIds) { imageUrls ->
            val enrichedItems = detail.items.map { item ->
                val resolvedImageUrl = item.imageUrl.ifBlank { imageUrls[item.productId].orEmpty() }
                if (resolvedImageUrl.isBlank()) {
                    Log.d(TAG, "Order item image fallback for productId=${item.productId}")
                }
                item.copy(imageUrl = resolvedImageUrl)
            }
            val heroItem = enrichedItems.maxByOrNull { item -> item.price }
            onResult(
                detail.copy(
                    items = enrichedItems,
                    heroImageUrl = heroItem?.imageUrl.orEmpty(),
                    heroImageRes = heroItem?.imageRes ?: R.drawable.img_pizza_time,
                ),
            )
        }
    }

    private fun enrichHistoryImages(
        orders: List<CustomerOrderHistoryItemUiModel>,
        onResult: (List<CustomerOrderHistoryItemUiModel>) -> Unit,
    ) {
        val missingProductIds = orders
            .filter { order -> order.imageUrl.isBlank() }
            .map { order -> order.heroProductId }
            .filter { productId -> productId.isNotBlank() }
            .toSet()

        loadProductImageUrls(missingProductIds) { imageUrls ->
            val enrichedOrders = orders.map { order ->
                val resolvedImageUrl = order.imageUrl.ifBlank { imageUrls[order.heroProductId].orEmpty() }
                if (order.heroProductId.isNotBlank() && resolvedImageUrl.isBlank()) {
                    Log.d(TAG, "Order history hero image fallback for productId=${order.heroProductId}")
                }
                order.copy(imageUrl = resolvedImageUrl)
            }
            onResult(enrichedOrders)
        }
    }

    private fun loadProductImageUrls(
        productIds: Set<String>,
        onResult: (Map<String, String>) -> Unit,
    ) {
        val ids = productIds
            .map { productId -> productId.trim() }
            .filter { productId -> productId.isNotBlank() }
            .distinct()

        if (ids.isEmpty()) {
            onResult(emptyMap())
            return
        }

        val images = mutableMapOf<String, String>()
        var remaining = ids.size

        fun completeOne() {
            remaining -= 1
            if (remaining == 0) {
                onResult(images)
            }
        }

        ids.forEach { productId ->
            firestore.collection("products").document(productId)
                .get()
                .addOnSuccessListener { doc ->
                    val imageUrl = doc.getString("imageUrl").orEmpty().trim()
                    if (imageUrl.isNotBlank()) {
                        images[productId] = imageUrl
                    } else {
                        Log.d(TAG, "Product image missing for productId=$productId")
                    }
                    completeOne()
                }
                .addOnFailureListener { error ->
                    Log.d(TAG, "Product image lookup failed for productId=$productId", error)
                    completeOne()
                }
        }
    }

    private fun buildItemDescription(map: Map<*, *>): String {
        val pieces = buildList {
            (map["selectedSize"] as? String)?.trim().orEmpty().takeIf { it.isNotBlank() }?.let { add(it) }
            (map["selectedCrust"] as? String)?.trim().orEmpty().takeIf { it.isNotBlank() }?.let { add(it) }
            val toppings = (map["selectedToppings"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            if (toppings.isNotEmpty()) {
                add(toppings.joinToString(", "))
            }
        }

        return if (pieces.isEmpty()) {
            (map["description"] as? String).orEmpty()
        } else {
            pieces.joinToString(" • ")
        }
    }

    private fun mapHistoryStatus(statusStr: String): CustomerOrderHistoryStatus {
        return when (statusStr.uppercase(Locale.US)) {
            "DELIVERED" -> CustomerOrderHistoryStatus.DELIVERED
            "CANCELLED", "CANCELED" -> CustomerOrderHistoryStatus.CANCELED
            else -> CustomerOrderHistoryStatus.IN_PROGRESS
        }
    }

    private fun mapStatusMessage(statusStr: String): String {
        return when (statusStr.uppercase(Locale.US)) {
            "DELIVERED" -> "Arrived safely"
            "DELIVERING" -> "On the way to you"
            "READY" -> "Ready for pickup"
            "BAKING" -> "In the oven"
            "PREPARING" -> "Being prepared"
            "CONFIRMED" -> "Order confirmed"
            STATUS_CANCELLED -> "Order cancelled"
            else -> "Order received"
        }
    }

    private fun buildItemSummary(rawItems: List<*>?): List<String> {
        if (rawItems.isNullOrEmpty()) return emptyList()
        return rawItems.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val name = map["name"] as? String ?: return@mapNotNull null
            val quantity = map["quantity"].asIntOrNull()?.coerceAtLeast(1) ?: 1
            "${quantity}x $name"
        }
    }

    private fun Timestamp.toHistoryDateString(): String {
        return SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US).format(Date(seconds * 1000))
    }

    private fun Timestamp.toDisplayTime(): String {
        return SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(seconds * 1000))
    }

    private fun List<*>?.toStatusHistoryUiModels(): List<CustomerOrderStatusHistoryUiModel> {
        if (this.isNullOrEmpty()) return emptyList()

        val entries = mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val createdAt = map["createdAt"] as? Timestamp
            OrderStatusHistoryEntry(
                status = (map["status"] as? String).orUnknownStatus(),
                actorRole = (map["actorRole"] as? String).orSystemRole(),
                note = (map["note"] as? String).orEmpty(),
                createdAt = createdAt,
            )
        }

        return entries
            .sortedWith(
                compareBy<OrderStatusHistoryEntry>(
                    { it.createdAt?.seconds ?: Long.MIN_VALUE },
                    { it.createdAt?.nanoseconds ?: 0 },
                ),
            )
            .map { entry ->
                CustomerOrderStatusHistoryUiModel(
                    status = entry.status,
                    actorRole = entry.actorRole,
                    note = entry.note,
                    timeText = entry.createdAt?.toHistoryDateString().orEmpty(),
                )
            }
    }

    private fun String?.orUnknownStatus(): String = this?.takeIf { it.isNotBlank() } ?: "Unknown"

    private fun String?.orSystemRole(): String = this?.takeIf { it.isNotBlank() } ?: "System"

    private fun String?.orNotProvided(): String = this?.takeIf { it.isNotBlank() } ?: "Not provided"

    private fun DocumentSnapshot.displayOrderCode(): String {
        return OrderCodeGenerator.displayOrderCode(
            orderCode = getString("orderCode"),
            orderId = id,
        )
    }

    private fun String?.toPaymentMethodLabel(): String {
        return when (this?.uppercase(Locale.US)) {
            "CASH_ON_DELIVERY", "CASH" -> "Cash on Delivery"
            else -> this?.takeIf { it.isNotBlank() } ?: "Cash on Delivery"
        }
    }

    private fun paymentStatusLabel(stored: String?, status: String): String {
        val normalized = stored?.uppercase(Locale.US)
        return when {
            normalized == "PAID" -> "Paid"
            normalized == "UNPAID" -> "Unpaid"
            status == "DELIVERED" -> "Paid"
            else -> "Unpaid"
        }
    }

    private data class OrderStatusHistoryEntry(
        val status: String,
        val actorRole: String,
        val note: String,
        val createdAt: Timestamp?,
    )

    private fun Any?.asDoubleOrNull(): Double? {
        return when (this) {
            is Number -> toDouble()
            is String -> toDoubleOrNull()
            else -> null
        }
    }

    private fun Any?.asIntOrNull(): Int? {
        return when (this) {
            is Number -> toInt()
            is String -> toIntOrNull()
            else -> null
        }
    }

    private fun Map<*, *>.firstString(vararg keys: String): String {
        keys.forEach { key ->
            val value = this[key] as? String
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return ""
    }

    private const val STATUS_PENDING = "PENDING"
    private const val STATUS_CANCELLED = "CANCELLED"
    private const val TAG = "CustomerOrderRepo"
}
