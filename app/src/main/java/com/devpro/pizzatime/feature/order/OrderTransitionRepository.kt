package com.devpro.pizzatime.feature.order

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object OrderTransitionRepository {

    const val STALE_ORDER_MESSAGE = "Order has already been updated. Please refresh."

    private const val STATUS_PENDING = "PENDING"
    private const val STATUS_CONFIRMED = "CONFIRMED"
    private const val STATUS_PREPARING = "PREPARING"
    private const val STATUS_BAKING = "BAKING"
    private const val STATUS_READY = "READY"
    private const val STATUS_ASSIGNED_TO_SHIPPER = "ASSIGNED_TO_SHIPPER"
    private const val STATUS_DELIVERING = "DELIVERING"
    private const val STATUS_DELIVERED = "DELIVERED"
    private const val STATUS_CANCELLED = "CANCELLED"

    private val firestore = FirebaseFirestore.getInstance()

    fun cancelByCustomer(
        orderId: String,
        customerId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        updateStatus(
            orderId = orderId,
            newStatus = STATUS_CANCELLED,
            allowedCurrentStatuses = setOf(STATUS_PENDING),
            actorRole = "CUSTOMER",
            actorId = customerId,
            note = "Order cancelled",
            onResult = onResult,
        )
    }

    fun confirmByStaff(
        orderId: String,
        staffId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        updateStatus(
            orderId = orderId,
            newStatus = STATUS_CONFIRMED,
            allowedCurrentStatuses = setOf(STATUS_PENDING),
            actorRole = "STAFF",
            actorId = staffId,
            note = "Order confirmed",
            onResult = onResult,
        )
    }

    fun cancelByStaff(
        orderId: String,
        staffId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        updateStatus(
            orderId = orderId,
            newStatus = STATUS_CANCELLED,
            allowedCurrentStatuses = setOf(STATUS_PENDING, STATUS_CONFIRMED),
            actorRole = "STAFF",
            actorId = staffId,
            note = "Order cancelled",
            onResult = onResult,
        )
    }

    fun updateByKitchen(
        orderId: String,
        newStatus: String,
        kitchenId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val expectedStatus = when (newStatus) {
            STATUS_PREPARING -> STATUS_CONFIRMED
            STATUS_BAKING -> STATUS_PREPARING
            STATUS_READY -> STATUS_BAKING
            else -> null
        }

        if (expectedStatus == null) {
            onResult(Result.failure(staleOrderException()))
            return
        }

        updateStatus(
            orderId = orderId,
            newStatus = newStatus,
            allowedCurrentStatuses = setOf(expectedStatus),
            actorRole = "KITCHEN",
            actorId = kitchenId,
            note = "Kitchen updated order to $newStatus",
            onResult = onResult,
        )
    }

    fun updateByShipper(
        orderId: String,
        newStatus: String,
        shipperId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        when (newStatus) {
            STATUS_ASSIGNED_TO_SHIPPER -> acceptByShipper(orderId, shipperId, onResult)
            STATUS_DELIVERING -> startDelivery(orderId, shipperId, onResult)
            STATUS_DELIVERED -> completeCashDelivery(orderId, shipperId, onResult)
            else -> onResult(Result.failure(staleOrderException()))
        }
    }

    private fun updateStatus(
        orderId: String,
        newStatus: String,
        allowedCurrentStatuses: Set<String>,
        actorRole: String,
        actorId: String,
        note: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val orderRef = firestore.collection("orders").document(orderId)
        firestore.runTransaction { transaction ->
            val order = transaction.get(orderRef)
            if (!order.exists()) {
                throw staleOrderException()
            }

            val currentStatus = order.getString("status").orEmpty()
            if (currentStatus !in allowedCurrentStatuses) {
                throw staleOrderException()
            }

            transaction.update(
                orderRef,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "statusHistory" to FieldValue.arrayUnion(
                        buildHistoryItem(
                            status = newStatus,
                            actorRole = actorRole,
                            actorId = actorId,
                            note = note,
                        ),
                    ),
                ),
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error.asCleanFailure())) }
    }

    private fun acceptByShipper(
        orderId: String,
        shipperId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val orderRef = firestore.collection("orders").document(orderId)
        firestore.runTransaction { transaction ->
            val order = transaction.get(orderRef)
            if (!order.exists()) {
                throw staleOrderException()
            }

            val currentStatus = order.getString("status").orEmpty()
            val currentShipperId = order.getString("shipperId").orEmpty()
            if (currentStatus != STATUS_READY || currentShipperId.isNotBlank()) {
                throw staleOrderException()
            }

            transaction.update(
                orderRef,
                mapOf(
                    "status" to STATUS_ASSIGNED_TO_SHIPPER,
                    "shipperId" to shipperId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "statusHistory" to FieldValue.arrayUnion(
                        buildHistoryItem(
                            status = STATUS_ASSIGNED_TO_SHIPPER,
                            actorRole = "SHIPPER",
                            actorId = shipperId,
                            note = "Shipper accepted order",
                        ),
                    ),
                ),
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error.asCleanFailure())) }
    }

    private fun startDelivery(
        orderId: String,
        shipperId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val orderRef = firestore.collection("orders").document(orderId)
        firestore.runTransaction { transaction ->
            val order = transaction.get(orderRef)
            if (!order.exists()) {
                throw staleOrderException()
            }

            val currentStatus = order.getString("status").orEmpty()
            val currentShipperId = order.getString("shipperId").orEmpty()
            val canStartFromReady = currentStatus == STATUS_READY && currentShipperId.isBlank()
            val canStartFromAssigned = currentStatus == STATUS_ASSIGNED_TO_SHIPPER &&
                (currentShipperId.isBlank() || currentShipperId == shipperId)
            if (!canStartFromReady && !canStartFromAssigned) {
                throw staleOrderException()
            }

            transaction.update(
                orderRef,
                mapOf(
                    "status" to STATUS_DELIVERING,
                    "shipperId" to shipperId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "statusHistory" to FieldValue.arrayUnion(
                        buildHistoryItem(
                            status = STATUS_DELIVERING,
                            actorRole = "SHIPPER",
                            actorId = shipperId,
                            note = "Shipper started delivery",
                        ),
                    ),
                ),
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error.asCleanFailure())) }
    }

    private fun completeCashDelivery(
        orderId: String,
        shipperId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val orderRef = firestore.collection("orders").document(orderId)

        firestore.runTransaction { transaction ->
            val order = transaction.get(orderRef)
            if (!order.exists()) {
                throw staleOrderException()
            }

            val currentStatus = order.getString("status").orEmpty()
            val currentShipperId = order.getString("shipperId").orEmpty()
            if (
                currentStatus != STATUS_DELIVERING ||
                currentShipperId.isNotBlank() && currentShipperId != shipperId
            ) {
                throw staleOrderException()
            }

            val total = order.getDouble("finalTotal") ?: order.getDouble("total") ?: 0.0
            transaction.update(
                orderRef,
                mapOf(
                    "status" to STATUS_DELIVERED,
                    "paymentMethod" to "CASH_ON_DELIVERY",
                    "paymentStatus" to "PAID",
                    "paidAt" to FieldValue.serverTimestamp(),
                    "deliveredAt" to FieldValue.serverTimestamp(),
                    "collectedByShipperId" to shipperId,
                    "collectedAmount" to total,
                    "cashCollected" to true,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "statusHistory" to FieldValue.arrayUnion(
                        buildHistoryItem(
                            status = STATUS_DELIVERED,
                            actorRole = "SHIPPER",
                            actorId = shipperId,
                            note = "Shipper delivered order and collected cash payment",
                        ),
                    ),
                ),
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error.asCleanFailure())) }
    }

    private fun Throwable.asCleanFailure(): Throwable {
        return if (this is OrderTransitionException) this else Exception(message ?: STALE_ORDER_MESSAGE)
    }

    private fun staleOrderException(): OrderTransitionException {
        return OrderTransitionException(STALE_ORDER_MESSAGE)
    }

    private fun buildHistoryItem(
        status: String,
        actorRole: String,
        actorId: String,
        note: String,
    ): HashMap<String, Any> {
        return hashMapOf(
            "status" to status,
            "actorRole" to actorRole,
            "actorId" to actorId,
            "note" to note,
            "createdAt" to Timestamp.now(),
        )
    }
}

class OrderTransitionException(message: String) : Exception(message)
