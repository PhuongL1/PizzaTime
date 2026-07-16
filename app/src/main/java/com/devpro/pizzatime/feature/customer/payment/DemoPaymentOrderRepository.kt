package com.devpro.pizzatime.feature.customer.payment

import com.devpro.pizzatime.feature.order.DeliveryHandoffStatus
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.devpro.pizzatime.feature.order.OrderPaymentHandoffParser
import com.devpro.pizzatime.feature.order.PaymentMethod
import com.devpro.pizzatime.feature.order.PaymentStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class DemoPaymentOrderSnapshot(
    val orderId: String,
    val displayOrderCode: String,
    val customerId: String,
    val orderStatus: String,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val paymentProvider: String?,
    val paymentAttemptId: String?,
    val paymentReference: String?,
    val deliveryHandoffStatus: DeliveryHandoffStatus,
    val paidAtMillis: Long,
)

object DemoPaymentOrderRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun listenOrder(
        orderId: String,
        customerId: String,
        onResult: (Result<DemoPaymentOrderSnapshot>) -> Unit,
    ): ListenerRegistration {
        return firestore.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }
                val document = snapshot
                if (document == null || !document.exists() || document.getString("customerId") != customerId) {
                    onResult(Result.failure(NoSuchElementException("Customer order unavailable")))
                    return@addSnapshotListener
                }
                onResult(Result.success(document.toDemoPaymentOrderSnapshot()))
            }
    }

    private fun DocumentSnapshot.toDemoPaymentOrderSnapshot(): DemoPaymentOrderSnapshot {
        val paymentMethod = OrderPaymentHandoffParser.parsePaymentMethod(
            getString(OrderPaymentHandoffParser.FIELD_PAYMENT_METHOD),
        )
        return DemoPaymentOrderSnapshot(
            orderId = id,
            displayOrderCode = OrderCodeGenerator.displayOrderCode(
                orderCode = getString("orderCode"),
                orderId = id,
            ),
            customerId = getString("customerId").orEmpty(),
            orderStatus = getString("status").orEmpty(),
            paymentMethod = paymentMethod,
            paymentStatus = OrderPaymentHandoffParser.parsePaymentStatus(
                method = paymentMethod,
                value = getString(OrderPaymentHandoffParser.FIELD_PAYMENT_STATUS),
            ),
            paymentProvider = getString(OrderPaymentHandoffParser.FIELD_PAYMENT_PROVIDER)?.trim()?.ifBlank { null },
            paymentAttemptId = getString(OrderPaymentHandoffParser.FIELD_PAYMENT_ATTEMPT_ID)?.trim()?.ifBlank { null },
            paymentReference = getString(OrderPaymentHandoffParser.FIELD_PAYMENT_REFERENCE)?.trim()?.ifBlank { null },
            deliveryHandoffStatus = OrderPaymentHandoffParser.parseDeliveryHandoffStatus(
                method = paymentMethod,
                value = getString(OrderPaymentHandoffParser.FIELD_DELIVERY_HANDOFF_STATUS),
            ),
            paidAtMillis = getTimestamp(OrderPaymentHandoffParser.FIELD_PAID_AT).toEpochMillis(),
        )
    }
}

private fun Timestamp?.toEpochMillis(): Long {
    return this?.toDate()?.time ?: 0L
}
