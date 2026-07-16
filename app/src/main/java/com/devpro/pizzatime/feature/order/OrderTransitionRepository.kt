package com.devpro.pizzatime.feature.order

import com.devpro.pizzatime.core.notification.NotificationDefaults
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object OrderTransitionRepository {

    const val STALE_ORDER_MESSAGE = "Order has already been updated. Please refresh."
    const val PAYMENT_NOT_CONFIRMED_MESSAGE = "Payment has not been confirmed yet."
    const val CUSTOMER_CONFIRMATION_REQUIRED_MESSAGE = "Customer confirmation is required before completing delivery."

    private const val STATUS_PENDING = "PENDING"
    private const val STATUS_CONFIRMED = "CONFIRMED"
    private const val STATUS_PREPARING = "PREPARING"
    private const val STATUS_BAKING = "BAKING"
    private const val STATUS_READY = "READY_FOR_DELIVERY"
    private const val STATUS_LEGACY_READY = "READY"
    private const val STATUS_ASSIGNED_TO_SHIPPER = "ASSIGNED_TO_SHIPPER"
    private const val STATUS_DELIVERING = "DELIVERING"
    private const val STATUS_DELIVERED = "DELIVERED"
    private const val STATUS_CANCELLED = "CANCELLED"

    private val firestore = FirebaseFirestore.getInstance()

    fun cancelByCustomer(
        orderId: String,
        customerId: String,
        reason: String? = null,
        onResult: (Result<Unit>) -> Unit,
    ) {
        updateStatus(
            orderId = orderId,
            newStatus = STATUS_CANCELLED,
            allowedCurrentStatuses = setOf(STATUS_PENDING),
            actorRole = "CUSTOMER",
            actorId = customerId,
            note = "Order cancelled",
            extraFields = cancellationFields("CUSTOMER", reason),
            onResult = onResult,
        )
    }

    fun confirmByStaff(
        orderId: String,
        staffId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val orderRef = firestore.collection("orders").document(orderId)
        firestore.runTransaction { transaction ->
            val order = transaction.get(orderRef)
            if (!order.exists()) {
                throw staleOrderException()
            }

            val paymentSnapshot = order.paymentSnapshot()
            if (!OrderPaymentHandoffPolicy.canStaffConfirmOrder(paymentSnapshot)) {
                throw if (paymentSnapshot.paymentMethod.isPrepaid() &&
                    paymentSnapshot.paymentStatus != PaymentStatus.PAID
                ) {
                    paymentNotConfirmedException()
                } else {
                    staleOrderException()
                }
            }

            transaction.update(
                orderRef,
                mapOf(
                    "status" to STATUS_CONFIRMED,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "statusHistory" to FieldValue.arrayUnion(
                        buildHistoryItem(
                            status = STATUS_CONFIRMED,
                            actorRole = "STAFF",
                            actorId = staffId,
                            note = "Order confirmed",
                        ),
                    ),
                ),
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error.asCleanFailure())) }
    }

    fun cancelByStaff(
        orderId: String,
        staffId: String,
        reason: String? = null,
        onResult: (Result<Unit>) -> Unit,
    ) {
        updateStatus(
            orderId = orderId,
            newStatus = STATUS_CANCELLED,
            allowedCurrentStatuses = setOf(STATUS_PENDING, STATUS_CONFIRMED),
            actorRole = "STAFF",
            actorId = staffId,
            note = "Order cancelled",
            extraFields = cancellationFields("STAFF", reason),
            onResult = onResult,
        )
    }

    fun updateByKitchen(
        orderId: String,
        newStatus: String,
        kitchenId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val normalizedStatus = when (newStatus) {
            STATUS_LEGACY_READY -> STATUS_READY
            else -> newStatus
        }
        val transition = when (normalizedStatus) {
            STATUS_PREPARING -> KitchenTransition(
                allowedCurrentStatuses = setOf(STATUS_CONFIRMED),
                kitchenStage = STATUS_PREPARING,
                progressPercent = 40,
            )
            STATUS_BAKING -> KitchenTransition(
                allowedCurrentStatuses = setOf(STATUS_PREPARING),
                kitchenStage = STATUS_BAKING,
                progressPercent = 75,
            )
            STATUS_READY -> KitchenTransition(
                allowedCurrentStatuses = setOf(STATUS_BAKING),
                kitchenStage = "READY",
                progressPercent = 100,
            )
            else -> null
        }

        if (transition == null) {
            onResult(Result.failure(staleOrderException()))
            return
        }

        updateStatus(
            orderId = orderId,
            newStatus = normalizedStatus,
            allowedCurrentStatuses = transition.allowedCurrentStatuses,
            actorRole = "KITCHEN",
            actorId = kitchenId,
            note = "Kitchen updated order to $normalizedStatus",
            extraFields = mapOf(
                "kitchenStage" to transition.kitchenStage,
                "kitchenProgressPercent" to transition.progressPercent,
                "kitchenUpdatedAt" to FieldValue.serverTimestamp(),
            ),
            onResult = onResult,
        )
    }

    fun cancelByKitchen(
        orderId: String,
        kitchenId: String,
        reason: String? = null,
        onResult: (Result<Unit>) -> Unit,
    ) {
        updateStatus(
            orderId = orderId,
            newStatus = STATUS_CANCELLED,
            allowedCurrentStatuses = setOf(STATUS_CONFIRMED, STATUS_PREPARING, STATUS_BAKING),
            actorRole = "KITCHEN",
            actorId = kitchenId,
            note = "Kitchen cancelled order",
            extraFields = buildMap {
                put("kitchenStage", STATUS_CANCELLED)
                put("kitchenUpdatedAt", FieldValue.serverTimestamp())
                putAll(cancellationFields("KITCHEN", reason))
            },
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
            STATUS_DELIVERED -> completeDelivery(orderId, shipperId, onResult)
            else -> onResult(Result.failure(staleOrderException()))
        }
    }

    fun markShipperArrived(
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
            val paymentSnapshot = order.paymentSnapshot()
            if (paymentSnapshot.deliveryHandoffStatus == DeliveryHandoffStatus.AWAITING_CUSTOMER) {
                return@runTransaction
            }
            if (!OrderPaymentHandoffPolicy.canShipperMarkArrived(paymentSnapshot, shipperId)) {
                throw staleOrderException()
            }
            transaction.update(
                orderRef,
                mapOf(
                    OrderPaymentHandoffParser.FIELD_DELIVERY_HANDOFF_STATUS to
                        DeliveryHandoffStatus.AWAITING_CUSTOMER.name,
                    OrderPaymentHandoffParser.FIELD_SHIPPER_ARRIVED_AT to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error.asCleanFailure())) }
    }

    fun confirmOrderReceived(
        orderId: String,
        customerId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val orderRef = firestore.collection("orders").document(orderId)
        firestore.runTransaction { transaction ->
            val order = transaction.get(orderRef)
            if (!order.exists()) {
                throw staleOrderException()
            }
            val paymentSnapshot = order.paymentSnapshot()
            if (
                paymentSnapshot.deliveryHandoffStatus == DeliveryHandoffStatus.CUSTOMER_CONFIRMED ||
                paymentSnapshot.deliveryHandoffStatus == DeliveryHandoffStatus.COMPLETED
            ) {
                return@runTransaction
            }
            if (!OrderPaymentHandoffPolicy.canCustomerConfirmReceipt(paymentSnapshot, customerId)) {
                throw staleOrderException()
            }
            transaction.update(
                orderRef,
                mapOf(
                    OrderPaymentHandoffParser.FIELD_DELIVERY_HANDOFF_STATUS to
                        DeliveryHandoffStatus.CUSTOMER_CONFIRMED.name,
                    OrderPaymentHandoffParser.FIELD_CUSTOMER_RECEIVED_AT to FieldValue.serverTimestamp(),
                    OrderPaymentHandoffParser.FIELD_CUSTOMER_RECEIPT_CONFIRMED_BY to customerId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error.asCleanFailure())) }
    }

    fun completePrepaidDelivery(
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
            val paymentSnapshot = order.paymentSnapshot()
            if (
                paymentSnapshot.deliveryHandoffStatus == DeliveryHandoffStatus.COMPLETED &&
                paymentSnapshot.orderStatus == STATUS_DELIVERED
            ) {
                return@runTransaction
            }
            if (!OrderPaymentHandoffPolicy.canShipperCompleteDelivery(paymentSnapshot, shipperId)) {
                throw if (
                    paymentSnapshot.paymentMethod.isPrepaid() &&
                    paymentSnapshot.paymentStatus == PaymentStatus.PAID &&
                    paymentSnapshot.orderStatus == STATUS_DELIVERING &&
                    paymentSnapshot.deliveryHandoffStatus != DeliveryHandoffStatus.CUSTOMER_CONFIRMED
                ) {
                    customerConfirmationRequiredException()
                } else {
                    staleOrderException()
                }
            }

            transaction.update(
                orderRef,
                mapOf(
                    "status" to STATUS_DELIVERED,
                    OrderPaymentHandoffParser.FIELD_DELIVERY_HANDOFF_STATUS to
                        DeliveryHandoffStatus.COMPLETED.name,
                    OrderPaymentHandoffParser.FIELD_DELIVERY_COMPLETED_AT to FieldValue.serverTimestamp(),
                    "deliveredAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "statusHistory" to FieldValue.arrayUnion(
                        buildHistoryItem(
                            status = STATUS_DELIVERED,
                            actorRole = "SHIPPER",
                            actorId = shipperId,
                            note = "Shipper completed prepaid delivery after customer confirmation",
                        ),
                    ),
                ),
            )
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error.asCleanFailure())) }
    }

    private fun updateStatus(
        orderId: String,
        newStatus: String,
        allowedCurrentStatuses: Set<String>,
        actorRole: String,
        actorId: String,
        note: String,
        extraFields: Map<String, Any> = emptyMap(),
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

            val updates = mutableMapOf<String, Any>(
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
                )
            updates.putAll(extraFields)
            transaction.update(orderRef, updates)
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
            if (currentStatus !in setOf(STATUS_READY, STATUS_LEGACY_READY) || currentShipperId.isNotBlank()) {
                throw staleOrderException()
            }
            val paymentSnapshot = order.paymentSnapshot()
            if (!OrderPaymentHandoffPolicy.canShipperStartDelivery(paymentSnapshot, shipperId)) {
                throw if (paymentSnapshot.paymentMethod.isPrepaid()) {
                    paymentNotConfirmedException()
                } else {
                    staleOrderException()
                }
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
            val canStartFromReady = currentStatus in setOf(STATUS_READY, STATUS_LEGACY_READY) &&
                currentShipperId.isBlank()
            val canStartFromAssigned = currentStatus == STATUS_ASSIGNED_TO_SHIPPER &&
                (currentShipperId.isBlank() || currentShipperId == shipperId)
            if (!canStartFromReady && !canStartFromAssigned) {
                throw staleOrderException()
            }
            val paymentSnapshot = order.paymentSnapshot()
            if (!OrderPaymentHandoffPolicy.canShipperStartDelivery(paymentSnapshot, shipperId)) {
                throw if (paymentSnapshot.paymentMethod.isPrepaid()) {
                    paymentNotConfirmedException()
                } else {
                    staleOrderException()
                }
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

    private fun completeDelivery(
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
            val paymentSnapshot = order.paymentSnapshot()
            when (paymentSnapshot.paymentMethod) {
                PaymentMethod.DEMO, PaymentMethod.VNPAY -> {
                    if (paymentSnapshot.deliveryHandoffStatus == DeliveryHandoffStatus.COMPLETED &&
                        paymentSnapshot.orderStatus == STATUS_DELIVERED
                    ) {
                        return@runTransaction
                    }
                    if (!OrderPaymentHandoffPolicy.canShipperCompleteDelivery(paymentSnapshot, shipperId)) {
                        throw if (paymentSnapshot.paymentStatus != PaymentStatus.PAID) {
                            paymentNotConfirmedException()
                        } else {
                            customerConfirmationRequiredException()
                        }
                    }
                    transaction.update(
                        orderRef,
                        mapOf(
                            "status" to STATUS_DELIVERED,
                            OrderPaymentHandoffParser.FIELD_DELIVERY_HANDOFF_STATUS to
                                DeliveryHandoffStatus.COMPLETED.name,
                            OrderPaymentHandoffParser.FIELD_DELIVERY_COMPLETED_AT to FieldValue.serverTimestamp(),
                            "deliveredAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                            "statusHistory" to FieldValue.arrayUnion(
                                buildHistoryItem(
                                    status = STATUS_DELIVERED,
                                    actorRole = "SHIPPER",
                                    actorId = shipperId,
                                    note = "Shipper completed prepaid delivery after customer confirmation",
                                ),
                            ),
                        ),
                    )
                    return@runTransaction
                }

                PaymentMethod.UNKNOWN -> throw staleOrderException()
                PaymentMethod.COD -> Unit
            }

            val total = order.getDouble("finalTotal") ?: order.getDouble("total") ?: 0.0
            transaction.update(
                orderRef,
                mapOf(
                    "status" to STATUS_DELIVERED,
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
                            note = "Shipper completed cash-on-delivery order",
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

    private fun paymentNotConfirmedException(): OrderTransitionException {
        return OrderTransitionException(PAYMENT_NOT_CONFIRMED_MESSAGE)
    }

    private fun customerConfirmationRequiredException(): OrderTransitionException {
        return OrderTransitionException(CUSTOMER_CONFIRMATION_REQUIRED_MESSAGE)
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

    private data class KitchenTransition(
        val allowedCurrentStatuses: Set<String>,
        val kitchenStage: String,
        val progressPercent: Int,
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.paymentSnapshot(): OrderPaymentHandoffSnapshot {
        return OrderPaymentHandoffParser.parse(
            orderStatus = getString("status"),
            paymentMethodValue = getString(OrderPaymentHandoffParser.FIELD_PAYMENT_METHOD),
            paymentStatusValue = getString(OrderPaymentHandoffParser.FIELD_PAYMENT_STATUS),
            handoffStatusValue = getString(OrderPaymentHandoffParser.FIELD_DELIVERY_HANDOFF_STATUS),
            customerId = getString("customerId"),
            shipperId = getString("shipperId"),
        )
    }

    private fun cancellationFields(
        cancelledBy: String,
        reason: String?,
    ): Map<String, Any> {
        val normalizedReason = reason
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }
            ?.let { value ->
                if (value.length <= NotificationDefaults.MAX_REASON_LENGTH) {
                    value
                } else {
                    value.take(NotificationDefaults.MAX_REASON_LENGTH - 1).trimEnd() + "\u2026"
                }
            }

        return buildMap {
            put("cancelledBy", cancelledBy)
            if (!normalizedReason.isNullOrBlank()) {
                put("cancellationReason", normalizedReason)
                put("statusReason", normalizedReason)
            }
        }
    }
}

class OrderTransitionException(message: String) : Exception(message)
