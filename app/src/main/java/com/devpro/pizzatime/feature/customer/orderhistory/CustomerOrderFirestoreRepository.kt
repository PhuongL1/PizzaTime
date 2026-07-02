package com.devpro.pizzatime.feature.customer.orderhistory

import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.customer.orderdetail.CustomerBillUiModel
import com.devpro.pizzatime.feature.customer.orderdetail.CustomerOrderDetailUiModel
import com.devpro.pizzatime.feature.customer.orderdetail.CustomerOrderItemUiModel
import com.google.firebase.Timestamp
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

    private fun DocumentSnapshot.toHistoryItem(): CustomerOrderHistoryItemUiModel {
        val statusStr = getString("status") ?: "PENDING"
        val total = getDouble("total") ?: 0.0
        val createdAt = getTimestamp("createdAt")
        val rawItems = get("items") as? List<*>

        return CustomerOrderHistoryItemUiModel(
            orderId = id,
            orderedAt = createdAt?.toHistoryDateString() ?: "",
            status = mapHistoryStatus(statusStr),
            itemSummary = buildItemSummary(rawItems),
            total = total,
            imageRes = if (!rawItems.isNullOrEmpty()) R.drawable.img_pizza_time else null,
        )
    }

    private fun DocumentSnapshot.toOrderDetail(): CustomerOrderDetailUiModel {
        val statusStr = getString("status") ?: "PENDING"
        val total = getDouble("total") ?: 0.0
        val subtotal = getDouble("subtotal") ?: 0.0
        val deliveryFee = getDouble("deliveryFee") ?: 0.0
        val discount = getDouble("discount") ?: 0.0
        val createdAt = getTimestamp("createdAt")
        val rawItems = get("items") as? List<*>

        return CustomerOrderDetailUiModel(
            orderId = id,
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
            deliveryAddressTitle = if (statusStr == "DELIVERED") "DELIVERED TO" else "DELIVERING TO",
            deliveryAddressLine1 = getString("deliveryAddress") ?: "",
            deliveryAddressLine2 = "",
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
            "CANCELLED" -> "Order cancelled"
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
}

