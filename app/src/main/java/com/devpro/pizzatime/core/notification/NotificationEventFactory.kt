package com.devpro.pizzatime.core.notification

import android.content.Context
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Locale

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
            val timestamp = map["createdAt"] as? Timestamp
            OrderHistoryEvent(
                status = status,
                createdAtMillis = timestamp.toEpochMillis(),
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
        val orderId = document.getString("orderId").orEmpty().ifBlank { null }
        val title = context.getString(R.string.notification_admin_product_review_title)
        val body = productName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { resolvedName ->
                context.getString(R.string.notification_admin_product_review_body, resolvedName)
            }
            ?: context.getString(R.string.notification_admin_product_review_body_generic)

        return AppNotification(
            id = "review:$reviewId",
            dedupeKey = "review:$reviewId",
            recipientRole = scope.role,
            recipientUserId = scope.userId,
            type = NotificationType.ADMIN_PRODUCT_REVIEW,
            title = title,
            body = body,
            orderId = orderId,
            reviewId = reviewId,
            createdAtMillis = document.getTimestamp("createdAt").toEpochMillis(),
            isRead = false,
            deepLinkType = NotificationDeepLink.ADMIN_REVIEW_DETAIL,
        )
    }

    fun normalizeStatus(status: String?): String {
        return when (status.orEmpty().trim().uppercase(Locale.US)) {
            "READY" -> "READY_FOR_DELIVERY"
            "READY_TO_DELIVER" -> "READY_FOR_DELIVERY"
            else -> status.orEmpty().trim().uppercase(Locale.US)
        }
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

        val type = notificationType(scope.role, event.status) ?: return null
        val deepLink = deepLinkType(scope.role, event.status)
        val dedupeKey = when (scope.role) {
            UserRole.STAFF ->
                "staff:new-order:$orderId:${event.createdAtMillis}"

            else ->
                "order:$orderId:status:${event.status}:${event.createdAtMillis}"
        }

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

    private fun notificationType(
        role: UserRole,
        status: String,
    ): NotificationType? {
        return when (role) {
            UserRole.CUSTOMER -> when (status) {
                "CONFIRMED" -> NotificationType.CUSTOMER_ORDER_CONFIRMED
                "PREPARING", "BAKING" -> NotificationType.CUSTOMER_ORDER_PREPARING
                "READY_FOR_DELIVERY" -> NotificationType.CUSTOMER_ORDER_READY
                "ASSIGNED_TO_SHIPPER" -> NotificationType.CUSTOMER_ORDER_STATUS_UPDATED
                "DELIVERING" -> NotificationType.CUSTOMER_DELIVERY_STARTED
                "DELIVERED" -> NotificationType.CUSTOMER_ORDER_DELIVERED
                "CANCELLED" -> NotificationType.CUSTOMER_ORDER_CANCELLED
                else -> null
            }

            UserRole.STAFF -> if (status == "PENDING") NotificationType.STAFF_NEW_ORDER else null
            UserRole.KITCHEN -> if (status == "CONFIRMED") NotificationType.KITCHEN_CONFIRMED_ORDER else null
            UserRole.SHIPPER -> if (status == "READY_FOR_DELIVERY") NotificationType.SHIPPER_READY_ORDER else null
            UserRole.ADMIN -> when (status) {
                "DELIVERED" -> NotificationType.ADMIN_ORDER_DELIVERED
                "CANCELLED" -> NotificationType.ADMIN_ORDER_CANCELLED
                else -> null
            }

            UserRole.GUEST -> null
        }
    }

    private fun deepLinkType(
        role: UserRole,
        status: String,
    ): NotificationDeepLink {
        return when (role) {
            UserRole.CUSTOMER -> when (status) {
                "DELIVERED", "CANCELLED" -> NotificationDeepLink.CUSTOMER_ORDER_DETAIL
                else -> NotificationDeepLink.CUSTOMER_ORDER_TRACKING
            }

            UserRole.STAFF -> NotificationDeepLink.STAFF_ORDER_DETAIL
            UserRole.KITCHEN -> NotificationDeepLink.KITCHEN_ORDER_DETAIL
            UserRole.SHIPPER -> NotificationDeepLink.SHIPPER_ORDER_DETAIL
            UserRole.ADMIN -> NotificationDeepLink.ADMIN_ORDER_DETAIL
            UserRole.GUEST -> NotificationDeepLink.NONE
        }
    }

    private fun Timestamp?.toEpochMillis(): Long {
        return this?.toDate()?.time ?: 0L
    }
}
