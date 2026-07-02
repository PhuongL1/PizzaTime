package com.devpro.pizzatime.feature.shipper

import com.devpro.pizzatime.feature.shipper.dashboard.ShipperDeliveryStatus
import com.devpro.pizzatime.feature.shipper.dashboard.ShipperDeliveryUiModel
import com.devpro.pizzatime.feature.shipper.detail.ShipperDeliveryDetailUiModel
import com.devpro.pizzatime.feature.shipper.detail.ShipperPaymentItemUiModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object ShipperOrderFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val shipperStatuses = listOf("READY", "ASSIGNED_TO_SHIPPER", "DELIVERING")

    fun loadOrders(onResult: (Result<List<ShipperDeliveryUiModel>>) -> Unit) {
        firestore.collection("orders")
            .whereIn("status", shipperStatuses)
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents.mapNotNull { it.toShipperDeliveryUiModel() }
                onResult(Result.success(orders))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun loadOrderDetail(
        orderId: String,
        onResult: (Result<Pair<ShipperDeliveryDetailUiModel, String>>) -> Unit,
    ) {
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(Result.failure(Exception("Order $orderId not found")))
                    return@addOnSuccessListener
                }
                val status = doc.getString("status") ?: "READY"
                onResult(Result.success(doc.toShipperDetailUiModel() to status))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        shipperId: String?,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (shipperId != null && newStatus == "ASSIGNED_TO_SHIPPER") {
            updates["shipperId"] = shipperId
        }
        firestore.collection("orders").document(orderId)
            .update(updates)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    private fun DocumentSnapshot.toShipperDeliveryUiModel(): ShipperDeliveryUiModel {
        val customerEmail = getString("customerEmail") ?: ""
        val customerName = getString("customerName")
            ?.takeIf { it.isNotBlank() && it != customerEmail }
            ?: customerEmail.substringBefore("@").ifBlank { "Customer" }
        val total = getDouble("total") ?: 0.0
        val statusStr = getString("status") ?: "READY"
        val paymentMethod = getString("paymentMethod") ?: "CASH"

        return ShipperDeliveryUiModel(
            orderId = id,
            customerName = customerName,
            address = getString("deliveryAddress") ?: "",
            etaLabel = "",
            paymentLabel = paymentMethod.uppercase(Locale.US),
            paymentAmount = String.format(Locale.US, "$%.2f", total),
            status = if (statusStr == "DELIVERING") ShipperDeliveryStatus.ACTIVE else ShipperDeliveryStatus.ASSIGNED,
        )
    }

    private fun DocumentSnapshot.toShipperDetailUiModel(): ShipperDeliveryDetailUiModel {
        val customerEmail = getString("customerEmail") ?: ""
        val customerName = getString("customerName")
            ?.takeIf { it.isNotBlank() && it != customerEmail }
            ?: customerEmail.substringBefore("@").ifBlank { "Customer" }
        val total = getDouble("total") ?: 0.0
        val rawItems = get("items") as? List<*>

        return ShipperDeliveryDetailUiModel(
            orderId = id,
            customerName = customerName,
            address = getString("deliveryAddress") ?: "",
            courierNote = getString("note") ?: "",
            paymentAmount = String.format(Locale.US, "$%.2f", total),
            paymentMethod = (getString("paymentMethod") ?: "CASH").uppercase(Locale.US),
            items = rawItems?.mapNotNull { it.toPaymentItem() } ?: emptyList(),
        )
    }

    private fun Any?.toPaymentItem(): ShipperPaymentItemUiModel? {
        val map = this as? Map<*, *> ?: return null
        val name = map["name"] as? String ?: return null
        val quantity = (map["quantity"] as? Long)?.toInt() ?: 1
        val unitPrice = map["unitPrice"] as? Double ?: 0.0
        return ShipperPaymentItemUiModel(
            name = "${quantity}x $name",
            price = String.format(Locale.US, "$%.2f", unitPrice * quantity),
        )
    }
}

