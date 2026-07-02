package com.devpro.pizzatime.feature.staff

import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.staff.dashboard.StaffFulfillmentType
import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderStatus
import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderUiModel
import com.devpro.pizzatime.feature.staff.detail.StaffOrderDetailItemUiModel
import com.devpro.pizzatime.feature.staff.detail.StaffOrderDetailTimelineUiModel
import com.devpro.pizzatime.feature.staff.detail.StaffOrderDetailUiModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        firestore.collection("orders").document(orderId)
            .update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
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

    private fun DocumentSnapshot.toStaffOrderUiModel(): StaffOrderUiModel {
        val customerEmail = getString("customerEmail") ?: ""
        val customerName = resolveCustomerName(getString("customerName"), customerEmail)
        val statusStr = getString("status") ?: "PENDING"
        val total = getDouble("total") ?: 0.0
        val createdAt = getTimestamp("createdAt")
        val orderType = getString("orderType") ?: "DELIVERY"
        val rawItems = get("items") as? List<*>

        return StaffOrderUiModel(
            orderId = id,
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
        val total = getDouble("total") ?: 0.0
        val createdAt = getTimestamp("createdAt")
        val rawItems = get("items") as? List<*>

        return StaffOrderDetailUiModel(
            orderId = id,
            receivedAgo = createdAt.toTimeAgo(),
            status = mapStatus(statusStr),
            customerName = customerName,
            customerPhone = "",
            deliveryAddress = getString("deliveryAddress") ?: "",
            estimatedDeliveryTime = "",
            paymentMethod = getString("paymentMethod") ?: "CASH",
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
}

