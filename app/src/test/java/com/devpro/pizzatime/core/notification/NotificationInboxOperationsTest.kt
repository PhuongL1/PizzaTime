package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.session.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationInboxOperationsTest {

    @Test
    fun markRead_decreasesUnreadCount() {
        val notifications = listOf(notification("first"), notification("second"))

        val updated = markNotificationRead(notifications, "first")

        assertEquals(1, unreadNotificationCount(updated))
    }

    @Test
    fun markAllRead_producesZeroUnread() {
        val notifications = listOf(notification("first"), notification("second"))

        val updated = markAllNotificationsRead(notifications)

        assertEquals(0, unreadNotificationCount(updated))
    }

    @Test
    fun unreadCount_keepsActualValueAboveBadgeLimit() {
        val notifications = List(100) { index -> notification("notification-$index") }

        assertEquals(100, unreadNotificationCount(notifications))
    }

    @Test
    fun storageKeys_isolateApplicationEditionAccountAndRole() {
        val customerScope = NotificationScope("com.devpro.pizzatime", "customer-a", UserRole.CUSTOMER)
        val otherApplication = customerScope.copy(applicationId = "com.devpro.pizzatime.staff")
        val otherAccount = customerScope.copy(userId = "customer-b")
        val otherRole = customerScope.copy(role = UserRole.STAFF)

        val customerKey = notificationInboxStorageKey(customerScope, AppEdition.CUSTOMER)

        assertNotEquals(customerKey, notificationInboxStorageKey(otherApplication, AppEdition.CUSTOMER))
        assertNotEquals(customerKey, notificationInboxStorageKey(customerScope, AppEdition.GUEST))
        assertNotEquals(customerKey, notificationInboxStorageKey(otherAccount, AppEdition.CUSTOMER))
        assertNotEquals(customerKey, notificationInboxStorageKey(otherRole, AppEdition.CUSTOMER))
    }

    @Test
    fun malformedPersistedData_failsSafely() {
        var failureLogged = false

        val notifications = parsePersistedInboxOrEmpty(
            raw = "not-json",
            decoder = { error("malformed") },
            onFailure = { failureLogged = true },
        )

        assertTrue(notifications.isEmpty())
        assertTrue(failureLogged)
    }

    private fun notification(id: String): AppNotification {
        return AppNotification(
            id = id,
            dedupeKey = "dedupe-$id",
            recipientRole = UserRole.CUSTOMER,
            recipientUserId = "customer-a",
            type = NotificationType.CUSTOMER_ORDER_STATUS_UPDATED,
            title = "Title",
            body = "Body",
            orderId = null,
            reviewId = null,
            createdAtMillis = 1L,
            isRead = false,
            deepLinkType = NotificationDeepLink.NONE,
        )
    }
}
