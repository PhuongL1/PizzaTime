package com.devpro.pizzatime.feature.staff

import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.staff.dashboard.StaffFulfillmentType
import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderStatus
import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderUiModel
import com.devpro.pizzatime.feature.staff.detail.StaffOrderDetailItemUiModel
import com.devpro.pizzatime.feature.staff.detail.StaffOrderDetailTimelineUiModel
import com.devpro.pizzatime.feature.staff.detail.StaffOrderDetailUiModel
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.devpro.pizzatime.feature.order.OrderTransitionRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object StaffOrderFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadOrders(onResult: (Result<List<StaffOrderUiModel>>) -> Unit) {
        firestore.collection("orders")
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents.mapNotNull { doc -> doc.toStaffOrderUiModel() }
                onResult(Result.success(orders))
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun listenOrders(onResult: (Result<List<StaffOrderUiModel>>) -> Unit): ListenerRegistration {
        return firestore.collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents
                    ?.mapNotNull { doc -> doc.toStaffOrderUiModel() }
                    ?: emptyList()
                onResult(Result.success(orders))
            }
    }

    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        if (newStatus != "CONFIRMED") {
            onResult(Result.failure(Exception(OrderTransitionRepository.STALE_ORDER_MESSAGE)))
            return
        }

        OrderTransitionRepository.confirmByStaff(
            orderId = orderId,
            staffId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            onResult = onResult,
        )
    }

    fun cancelOrder(
        orderId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        OrderTransitionRepository.cancelByStaff(
            orderId = orderId,
            staffId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            onResult = onResult,
        )
    }

    fun loadOrderDetail(
        orderId: String,
        onResult: (Result<StaffOrderDetailUiModel>) -> Unit,
    ) {
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(Result.failure(Exception("Order $orderId not found")))
                    return@addOnSuccessListener
                }
                onResult(Result.success(doc.toStaffOrderDetailUiModel()))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    private fun DocumentSnapshot.toStaffOrderUiModel(): StaffOrderUiModel? {
        val customerEmail = getString("customerEmail") ?: ""
        val customerName = resolveCustomerName(getString("customerName"), customerEmail)
        val statusStr = getString("status") ?: "PENDING"
        if (statusStr == STATUS_CANCELLED) {
            return null
        }
        val total = getDouble("finalTotal") ?: getDouble("total") ?: 0.0
        val createdAt = getTimestamp("createdAt")
        val orderType = getString("orderType") ?: "DELIVERY"
        val rawItems = get("items") as? List<*>

        return StaffOrderUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            customerName = customerName,
            timeAgo = createdAt.toTimeAgo(),
            fulfillmentType = mapFulfillmentType(orderType),
            orderSummary = buildOrderSummary(rawItems),
            price = String.format(Locale.US, "$%.2f", total),
            status = mapStatus(statusStr),
        )
    }

    private fun DocumentSnapshot.toStaffOrderDetailUiModel(): StaffOrderDetailUiModel {
        val customerEmail = getString("customerEmail") ?: ""
        val customerName = resolveCustomerName(getString("customerName"), customerEmail)
        val statusStr = getString("status") ?: "PENDING"
        val total = getDouble("finalTotal") ?: getDouble("total") ?: 0.0
        val createdAt = getTimestamp("createdAt")
        val rawItems = get("items") as? List<*>

        return StaffOrderDetailUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            receivedAgo = createdAt.toTimeAgo(),
            status = mapStatus(statusStr),
            storeName = getString("storeName").orNotProvided(),
            pickupAddress = getString("pickupAddress").orNotProvided(),
            storePhone = getString("storePhone").orNotProvided(),
            distanceKm = getDouble("distanceKm"),
            deliveryFee = getDouble("deliveryFee") ?: 0.0,
            customerName = customerName,
            customerPhone = getString("customerPhone").orNotProvided(),
            deliveryAddress = getString("deliveryAddress").orNotProvided(),
            estimatedDeliveryTime = "",
            paymentMethod = getString("paymentMethod").toPaymentMethodLabel(),
            paymentStatus = paymentStatusLabel(
                stored = getString("paymentStatus"),
                status = statusStr,
            ),
            cashCollected = getBoolean("cashCollected") == true,
            collectedByShipperId = getString("collectedByShipperId").orEmpty(),
            collectedAmount = getDouble("collectedAmount") ?: 0.0,
            paymentTotal = total,
            deliveryNote = getString("note") ?: "",
            items = rawItems?.mapNotNull { it.toDetailItem() } ?: emptyList(),
            timeline = StaffOrderDetailTimelineUiModel(
                orderPlacedTime = createdAt?.toDisplayTime() ?: "",
                confirmedTime = null,
                preparingTime = null,
                readyTime = null,
            ),
        )
    }

    private fun Any?.toDetailItem(): StaffOrderDetailItemUiModel? {
        val map = this as? Map<*, *> ?: return null
        val name = map["name"] as? String ?: return null
        val quantity = (map["quantity"] as? Long)?.toInt() ?: 1
        val unitPrice = map["unitPrice"] as? Double ?: 0.0
        val description = map["description"] as? String ?: ""
        return StaffOrderDetailItemUiModel(
            name = name,
            description = description,
            quantity = quantity,
            price = unitPrice,
            imageRes = R.drawable.img_pizza_time,
        )
    }

    private fun resolveCustomerName(stored: String?, email: String): String {
        if (!stored.isNullOrBlank() && stored != email) return stored
        return email.substringBefore("@").ifBlank { "Customer" }
    }

    private fun mapStatus(statusStr: String): StaffOrderStatus {
        return when (statusStr.uppercase(Locale.US)) {
            "PENDING" -> StaffOrderStatus.PENDING
            "CONFIRMED" -> StaffOrderStatus.CONFIRMED
            "PREPARING" -> StaffOrderStatus.PREPARING
            "READY", "DELIVERING", "DELIVERED" -> StaffOrderStatus.READY
            STATUS_CANCELLED -> StaffOrderStatus.CANCELLED
            else -> StaffOrderStatus.PENDING
        }
    }

    private fun mapFulfillmentType(orderType: String): StaffFulfillmentType {
        return if (orderType.uppercase(Locale.US) == "DELIVERY") {
            StaffFulfillmentType.DELIVERY
        } else {
            StaffFulfillmentType.COLLECTION
        }
    }

    private fun buildOrderSummary(rawItems: List<*>?): String {
        if (rawItems.isNullOrEmpty()) return ""
        val firstName = (rawItems.firstOrNull() as? Map<*, *>)?.get("name") as? String ?: ""
        return if (rawItems.size == 1) {
            "1x $firstName"
        } else {
            "1x $firstName +${rawItems.size - 1} more"
        }
    }

    private fun Timestamp?.toTimeAgo(): String {
        if (this == null) return ""
        val diffMs = System.currentTimeMillis() - seconds * 1000
        val diffMins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        return when {
            diffMins < 1 -> "Just now"
            diffMins < 60 -> "$diffMins MINS AGO"
            else -> "${TimeUnit.MINUTES.toHours(diffMins)} HRS AGO"
        }
    }

    private fun Timestamp.toDisplayTime(): String {
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date(seconds * 1000))
    }

    private fun String?.orNotProvided(): String = this?.takeIf { it.isNotBlank() } ?: NOT_PROVIDED

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

    private const val STATUS_CANCELLED = "CANCELLED"
    private const val NOT_PROVIDED = "Not provided"
}

