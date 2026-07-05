package com.devpro.pizzatime.feature.customer.orderhistory

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
                onResult(Result.success(orders))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun loadOrderDetail(
        orderId: String,
        onResult: (Result<CustomerOrderDetailUiModel>) -> Unit,
    ) {
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(Result.failure(Exception("Order $orderId not found")))
                    return@addOnSuccessListener
                }
                onResult(Result.success(doc.toOrderDetail()))
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
            onResult = onResult,
        )
    }

    private fun DocumentSnapshot.toHistoryItem(): CustomerOrderHistoryItemUiModel {
        val statusStr = getString("status") ?: "PENDING"
        val total = getDouble("total") ?: 0.0
        val createdAt = getTimestamp("createdAt")
        val rawItems = get("items") as? List<*>

        return CustomerOrderHistoryItemUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            orderedAt = createdAt?.toHistoryDateString() ?: "",
            status = mapHistoryStatus(statusStr),
            itemSummary = buildItemSummary(rawItems),
            total = total,
            imageRes = if (!rawItems.isNullOrEmpty()) R.drawable.img_pizza_time else null,
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

        return CustomerOrderDetailUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            statusLabel = statusStr,
            orderTime = createdAt?.toDisplayTime() ?: "",
            heroImageRes = R.drawable.img_pizza_time,
            heroMessage = mapStatusMessage(statusStr),
            items = rawItems?.mapNotNull { it.toOrderItem() } ?: emptyList(),
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
        val quantity = (map["quantity"] as? Long)?.toInt() ?: 1
        val unitPrice = map["unitPrice"] as? Double ?: 0.0
        return CustomerOrderItemUiModel(
            quantity = quantity,
            name = name,
            description = "",
            price = unitPrice * quantity,
            imageRes = null,
        )
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
            val quantity = (map["quantity"] as? Long)?.toInt() ?: 1
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

    private const val STATUS_PENDING = "PENDING"
    private const val STATUS_CANCELLED = "CANCELLED"
}

