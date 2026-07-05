package com.devpro.pizzatime.feature.shipper

import com.devpro.pizzatime.feature.order.OrderTransitionRepository
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.devpro.pizzatime.feature.shipper.dashboard.ShipperDeliveryStatus
import com.devpro.pizzatime.feature.shipper.dashboard.ShipperDeliveryUiModel
import com.devpro.pizzatime.feature.shipper.detail.ShipperDeliveryDetailUiModel
import com.devpro.pizzatime.feature.shipper.detail.ShipperPaymentItemUiModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    fun listenOrders(onResult: (Result<List<ShipperDeliveryUiModel>>) -> Unit): ListenerRegistration {
        return firestore.collection("orders")
            .whereIn("status", shipperStatuses)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents
                    ?.mapNotNull { it.toShipperDeliveryUiModel() }
                    ?: emptyList()
                onResult(Result.success(orders))
            }
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
        val currentShipperId = shipperId
        if (currentShipperId.isNullOrBlank()) {
            onResult(Result.failure(Exception(OrderTransitionRepository.STALE_ORDER_MESSAGE)))
            return
        }

        OrderTransitionRepository.updateByShipper(
            orderId = orderId,
            newStatus = newStatus,
            shipperId = currentShipperId,
            onResult = onResult,
        )
    }

    private fun DocumentSnapshot.toShipperDeliveryUiModel(): ShipperDeliveryUiModel {
        val customerEmail = getString("customerEmail") ?: ""
        val customerName = getString("customerName")
            ?.takeIf { it.isNotBlank() && it != customerEmail }
            ?: customerEmail.substringBefore("@").ifBlank { "Customer" }
        val total = getDouble("finalTotal") ?: getDouble("total") ?: 0.0
        val statusStr = getString("status") ?: "READY"
        val paymentMethod = getString("paymentMethod").toPaymentMethodLabel()

        return ShipperDeliveryUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            customerName = customerName,
            address = getString("deliveryAddress").orNotProvided(),
            etaLabel = "",
            paymentLabel = paymentMethod.uppercase(Locale.US),
            paymentAmount = String.format(Locale.US, "$%.2f", total),
            status = if (statusStr == "DELIVERING") ShipperDeliveryStatus.ACTIVE else ShipperDeliveryStatus.ASSIGNED,
            shipperId = getString("shipperId").orEmpty(),
        )
    }

    private fun DocumentSnapshot.toShipperDetailUiModel(): ShipperDeliveryDetailUiModel {
        val customerEmail = getString("customerEmail") ?: ""
        val customerName = getString("customerName")
            ?.takeIf { it.isNotBlank() && it != customerEmail }
            ?: customerEmail.substringBefore("@").ifBlank { "Customer" }
        val total = getDouble("finalTotal") ?: getDouble("total") ?: 0.0
        val deliveryFee = getDouble("deliveryFee") ?: 0.0
        val rawItems = get("items") as? List<*>

        return ShipperDeliveryDetailUiModel(
            orderId = id,
            displayOrderCode = displayOrderCode(),
            storeName = getString("storeName").orNotProvided(),
            pickupAddress = getString("pickupAddress").orNotProvided(),
            pickupLat = getDouble("pickupLat"),
            pickupLng = getDouble("pickupLng"),
            storePhone = getString("storePhone").orNotProvided(),
            customerName = customerName,
            customerPhone = getString("customerPhone").orNotProvided(),
            address = getString("deliveryAddress").orNotProvided(),
            deliveryLat = getDouble("deliveryLat"),
            deliveryLng = getDouble("deliveryLng"),
            distanceKm = getDouble("distanceKm"),
            deliveryFee = String.format(Locale.US, "$%.2f", deliveryFee),
            courierNote = getString("note") ?: "",
            paymentAmount = String.format(Locale.US, "$%.2f", total),
            paymentMethod = getString("paymentMethod").toPaymentMethodLabel(),
            paymentStatus = paymentStatusLabel(
                stored = getString("paymentStatus"),
                status = getString("status").orEmpty(),
            ),
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

    private const val NOT_PROVIDED = "Not provided"
}
