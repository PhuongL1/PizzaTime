package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.core.session.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationRoutingPolicyTest {

    @Test
    fun validCustomerNotification_resolvesExactDestination() {
        val request = notification.toRoutingRequest(scope.applicationId)

        assertEquals(
            NotificationRoutingDecision.ALLOWED,
            validateNotificationRouting(requireNotNull(request), scope, notification),
        )
    }

    @Test
    fun wrongRoleOrAccountOrApplication_isRejected() {
        val request = requireNotNull(notification.toRoutingRequest(scope.applicationId))

        assertEquals(
            NotificationRoutingDecision.WRONG_SCOPE,
            validateNotificationRouting(request.copy(recipientRole = UserRole.STAFF), scope, notification),
        )
        assertEquals(
            NotificationRoutingDecision.WRONG_SCOPE,
            validateNotificationRouting(request.copy(recipientUserId = "customer-b"), scope, notification),
        )
        assertEquals(
            NotificationRoutingDecision.WRONG_SCOPE,
            validateNotificationRouting(request.copy(applicationId = "other.application"), scope, notification),
        )
    }

    @Test
    fun missingOrMismatchedInboxEvent_isRejected() {
        val request = requireNotNull(notification.toRoutingRequest(scope.applicationId))

        assertEquals(
            NotificationRoutingDecision.NOT_FOUND,
            validateNotificationRouting(request, scope, null),
        )
        assertEquals(
            NotificationRoutingDecision.MISMATCHED_EVENT,
            validateNotificationRouting(request.copy(orderId = "other-order"), scope, notification),
        )
    }

    private companion object {
        val scope = NotificationScope("com.devpro.pizzatime", "customer-a", UserRole.CUSTOMER)
        val notification = AppNotification(
            id = "notification-a",
            dedupeKey = "dedupe-a",
            recipientRole = UserRole.CUSTOMER,
            recipientUserId = "customer-a",
            type = NotificationType.CUSTOMER_ORDER_CONFIRMED,
            title = "Order confirmed",
            body = "Order updated",
            orderId = "order-a",
            reviewId = null,
            createdAtMillis = 1000L,
            isRead = false,
            deepLinkType = NotificationDeepLink.CUSTOMER_ORDER_TRACKING,
        )
    }
}
