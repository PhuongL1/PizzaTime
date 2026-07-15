package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.core.session.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationFcmPayloadTest {

    @Test
    fun canonicalOrderPayload_isAccepted() {
        val payload = parseNotificationFcmPayload(validData(), scope)

        assertEquals(NotificationType.CUSTOMER_ORDER_CONFIRMED, payload?.type)
        assertEquals("order-a", payload?.orderId)
        assertEquals("order:order-a:status:CONFIRMED:1000", payload?.dedupeKey)
    }

    @Test
    fun malformedPayload_isRejected() {
        assertNull(parseNotificationFcmPayload(mapOf("type" to "unknown"), scope))
    }

    @Test
    fun unsupportedEventType_isRejected() {
        val adminScope = scope.copy(
            applicationId = "com.devpro.pizzatime.admin",
            userId = "admin-a",
            role = UserRole.ADMIN,
        )
        val data = validData(adminScope).toMutableMap().apply {
            this[FCM_KEY_TYPE] = NotificationType.ADMIN_ORDER_REVIEW.name
        }

        assertNull(parseNotificationFcmPayload(data, adminScope))
    }

    @Test
    fun payloadForAnotherApplicationOrRole_isRejected() {
        val wrongApplication = validData().toMutableMap().apply {
            this[FCM_KEY_APPLICATION_ID] = "com.devpro.pizzatime.staff"
        }
        val wrongRole = validData().toMutableMap().apply {
            this[FCM_KEY_RECIPIENT_ROLE] = UserRole.STAFF.name
        }

        assertNull(parseNotificationFcmPayload(wrongApplication, scope))
        assertNull(parseNotificationFcmPayload(wrongRole, scope))
    }

    @Test
    fun nonCanonicalDedupeKey_isRejected() {
        val data = validData().toMutableMap().apply {
            this[FCM_KEY_DEDUPE_KEY] = "remote-fallback"
        }

        assertNull(parseNotificationFcmPayload(data, scope))
    }

    private fun validData(targetScope: NotificationScope = scope): Map<String, String> {
        return mapOf(
            FCM_KEY_APPLICATION_ID to targetScope.applicationId,
            FCM_KEY_RECIPIENT_USER_ID to targetScope.userId,
            FCM_KEY_RECIPIENT_ROLE to targetScope.role.name,
            FCM_KEY_TYPE to NotificationType.CUSTOMER_ORDER_CONFIRMED.name,
            FCM_KEY_EVENT_MILLIS to "1000",
            FCM_KEY_DEDUPE_KEY to "order:order-a:status:CONFIRMED:1000",
            FCM_KEY_ORDER_ID to "order-a",
            FCM_KEY_ORDER_STATUS to "CONFIRMED",
        )
    }

    private companion object {
        val scope = NotificationScope(
            applicationId = "com.devpro.pizzatime",
            userId = "customer-a",
            role = UserRole.CUSTOMER,
        )
    }
}
