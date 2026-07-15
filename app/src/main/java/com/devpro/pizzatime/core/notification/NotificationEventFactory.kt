package com.devpro.pizzatime.core.notification

import android.content.Context
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.google.firebase.firestore.DocumentSnapshot

data class OrderHistoryEvent(
    val status: String,
    val createdAtMillis: Long,
)

object NotificationEventFactory {

    fun createOrderNotifications(
        context: Context,
        scope: NotificationScope,
        document: DocumentSnapshot,
        historyEvents: List<OrderHistoryEvent>,
    ): List<AppNotification> {
        if (historyEvents.isEmpty()) {
            return emptyList()
        }

        val orderId = document.id
        val orderCode = OrderCodeGenerator.displayOrderCode(
            orderCode = document.getString("orderCode"),
            orderId = orderId,
        )
        return historyEvents.mapNotNull { event ->
            buildOrderNotification(
                context = context,
                scope = scope,
                orderId = orderId,
                orderCode = orderCode,
                event = event,
                currentStatus = normalizeStatus(document.getString("status")),
                reason = resolveCancellationReason(document),
            )
        }
    }

    fun latestHistoryAtMillis(document: DocumentSnapshot): Long {
        return historyEvents(document).maxOfOrNull { event -> event.createdAtMillis } ?: 0L
    }

    fun historyEvents(document: DocumentSnapshot): List<OrderHistoryEvent> {
        val rawHistory = document.get("statusHistory") as? List<*>
        if (rawHistory.isNullOrEmpty()) {
            return emptyList()
        }

        return rawHistory.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val status = normalizeStatus(map["status"] as? String)
            if (status.isBlank()) {
                return@mapNotNull null
            }
            OrderHistoryEvent(
                status = status,
                createdAtMillis = notificationEpochMillis(map["createdAt"]),
            )
        }.sortedBy { event -> event.createdAtMillis }
    }

    fun resolveCancellationReason(document: DocumentSnapshot): String? {
        val candidates = listOf(
            document.getString("cancellationReason"),
            document.getString("cancelReason"),
            document.getString("cancelledReason"),
            document.getString("statusReason"),
            document.getString("cancellationNote"),
        )

        return candidates.firstNotNullOfOrNull { value ->
            value?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { nonBlank ->
                    if (nonBlank.length <= NotificationDefaults.MAX_REASON_LENGTH) {
                        nonBlank
                    } else {
                        nonBlank.take(NotificationDefaults.MAX_REASON_LENGTH - 1).trimEnd() + "\u2026"
                    }
                }
        }
    }

    fun createProductReviewNotification(
        context: Context,
        scope: NotificationScope,
        document: DocumentSnapshot,
        productName: String?,
    ): AppNotification? {
        val reviewId = document.getString("reviewId").orEmpty().ifBlank { document.id }
        if (reviewId.isBlank()) {
            return null
        }
        return buildProductReviewNotification(
            context = context,
            scope = scope,
            reviewId = reviewId,
            orderId = document.getString("orderId").orEmpty().ifBlank { null },
            productName = productName,
            createdAtMillis = notificationEpochMillis(document.get("createdAt")),
        )
    }

    fun createFcmNotification(
        context: Context,
        scope: NotificationScope,
        data: Map<String, String>,
    ): AppNotification? {
        val payload = parseNotificationFcmPayload(data, scope) ?: return null
        if (payload.type == NotificationType.ADMIN_PRODUCT_REVIEW) {
            return buildProductReviewNotification(
                context = context,
                scope = scope,
                reviewId = payload.reviewId ?: return null,
                orderId = payload.orderId,
                productName = payload.productName,
                createdAtMillis = payload.eventMillis,
            )
        }

        val orderId = payload.orderId ?: return null
        val status = payload.orderStatus ?: return null
        val orderCode = OrderCodeGenerator.displayOrderCode(
            orderCode = payload.orderCode,
            orderId = orderId,
        )
        return buildOrderNotification(
            context = context,
            scope = scope,
            orderId = orderId,
            orderCode = orderCode,
            event = OrderHistoryEvent(
                status = status,
                createdAtMillis = payload.eventMillis,
            ),
            currentStatus = status,
            reason = payload.cancellationReason,
        )?.takeIf { notification ->
            notification.type == payload.type && notification.dedupeKey == payload.dedupeKey
        }
    }

    private fun buildProductReviewNotification(
        context: Context,
        scope: NotificationScope,
        reviewId: String,
        orderId: String?,
        productName: String?,
        createdAtMillis: Long,
    ): AppNotification {
        val title = context.getString(R.string.notification_admin_product_review_title)
        val body = productName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { resolvedName ->
                context.getString(R.string.notification_admin_product_review_body, resolvedName)
            }
            ?: context.getString(R.string.notification_admin_product_review_body_generic)

        return AppNotification(
            id = canonicalReviewNotificationDedupeKey(reviewId),
            dedupeKey = canonicalReviewNotificationDedupeKey(reviewId),
            recipientRole = scope.role,
            recipientUserId = scope.userId,
            type = NotificationType.ADMIN_PRODUCT_REVIEW,
            title = title,
            body = body,
            orderId = orderId,
            reviewId = reviewId,
            createdAtMillis = createdAtMillis,
            isRead = false,
            deepLinkType = NotificationDeepLink.ADMIN_REVIEW_DETAIL,
        )
    }

    fun normalizeStatus(status: String?): String {
        return normalizeNotificationStatus(status)
    }

    private fun buildOrderNotification(
        context: Context,
        scope: NotificationScope,
        orderId: String,
        orderCode: String,
        event: OrderHistoryEvent,
        currentStatus: String,
        reason: String?,
    ): AppNotification? {
        val titleBody = when (scope.role) {
            UserRole.CUSTOMER -> customerTitleBody(context, orderCode, event.status, reason)
            UserRole.STAFF -> if (event.status == "PENDING") {
                context.getString(R.string.notification_staff_new_order_title) to
                    context.getString(R.string.notification_staff_new_order_body, orderCode)
            } else {
                null
            }

            UserRole.KITCHEN -> if (event.status == "CONFIRMED") {
                context.getString(R.string.notification_kitchen_confirmed_title) to
                    context.getString(R.string.notification_kitchen_confirmed_body, orderCode)
            } else {
                null
            }

            UserRole.SHIPPER -> if (event.status == "READY_FOR_DELIVERY") {
                context.getString(R.string.notification_shipper_ready_title) to
                    context.getString(R.string.notification_shipper_ready_body, orderCode)
            } else {
                null
            }

            UserRole.ADMIN -> adminTitleBody(context, orderCode, event.status, reason)
            UserRole.GUEST -> null
        } ?: return null

        val type = notificationTypeForOrderTransition(scope.role, event.status) ?: return null
        val deepLink = notificationDeepLinkForOrder(scope.role, event.status)
        val dedupeKey = canonicalOrderNotificationDedupeKey(
            role = scope.role,
            orderId = orderId,
            status = event.status,
            eventMillis = event.createdAtMillis,
        )

        if (scope.role == UserRole.STAFF && currentStatus != "PENDING") {
            return null
        }

        return AppNotification(
            id = dedupeKey,
            dedupeKey = dedupeKey,
            recipientRole = scope.role,
            recipientUserId = scope.userId,
            type = type,
            title = titleBody.first,
            body = titleBody.second,
            orderId = orderId,
            reviewId = null,
            createdAtMillis = event.createdAtMillis,
            isRead = false,
            deepLinkType = deepLink,
        )
    }

    private fun customerTitleBody(
        context: Context,
        orderCode: String,
        status: String,
        reason: String?,
    ): Pair<String, String>? {
        return when (status) {
            "CONFIRMED" -> context.getString(R.string.notification_customer_confirmed_title) to
                context.getString(R.string.notification_customer_confirmed_body, orderCode)

            "PREPARING", "BAKING" -> context.getString(R.string.notification_customer_preparing_title) to
                context.getString(R.string.notification_customer_preparing_body, orderCode)

            "READY_FOR_DELIVERY" -> context.getString(R.string.notification_customer_ready_title) to
                context.getString(R.string.notification_customer_ready_body, orderCode)

            "ASSIGNED_TO_SHIPPER" -> context.getString(R.string.notification_customer_updated_title) to
                context.getString(
                    R.string.notification_customer_updated_body,
                    orderCode,
                    context.getString(R.string.notification_status_assigned_to_shipper),
                )

            "DELIVERING" -> context.getString(R.string.notification_customer_delivering_title) to
                context.getString(R.string.notification_customer_delivering_body, orderCode)

            "DELIVERED" -> context.getString(R.string.notification_customer_delivered_title) to
                context.getString(R.string.notification_customer_delivered_body, orderCode)

            "CANCELLED" -> if (reason.isNullOrBlank()) {
                context.getString(R.string.notification_customer_cancelled_title) to
                    context.getString(R.string.notification_customer_cancelled_body, orderCode)
            } else {
                context.getString(R.string.notification_customer_cancelled_title) to
                    context.getString(R.string.notification_customer_cancelled_body_with_reason, orderCode, reason)
            }

            else -> null
        }
    }

    private fun adminTitleBody(
        context: Context,
        orderCode: String,
        status: String,
        reason: String?,
    ): Pair<String, String>? {
        return when (status) {
            "DELIVERED" -> context.getString(R.string.notification_admin_delivered_title) to
                context.getString(R.string.notification_admin_delivered_body, orderCode)

            "CANCELLED" -> if (reason.isNullOrBlank()) {
                context.getString(R.string.notification_admin_cancelled_title) to
                    context.getString(R.string.notification_admin_cancelled_body, orderCode)
            } else {
                context.getString(R.string.notification_admin_cancelled_title) to
                    context.getString(R.string.notification_admin_cancelled_body_with_reason, orderCode, reason)
            }

            else -> null
        }
    }

}
