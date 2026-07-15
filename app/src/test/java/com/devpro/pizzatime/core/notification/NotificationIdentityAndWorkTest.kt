package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.core.session.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class NotificationIdentityAndWorkTest {

    @Test
    fun notificationId_isStableForSameEventAndDifferentForUnrelatedEvents() {
        val first = systemNotificationId(scope, "event-a")

        assertEquals(first, systemNotificationId(scope, "event-a"))
        assertNotEquals(first, systemNotificationId(scope, "event-b"))
        assertNotEquals(
            systemNotificationIntentToken(scope, "event-a"),
            systemNotificationIntentToken(scope, "event-b"),
        )
    }

    @Test
    fun notificationIdentity_isIsolatedByApplicationAccountAndRole() {
        val id = systemNotificationId(scope, "event-a")

        assertNotEquals(id, systemNotificationId(scope.copy(applicationId = "other.app"), "event-a"))
        assertNotEquals(id, systemNotificationId(scope.copy(userId = "customer-b"), "event-a"))
        assertNotEquals(id, systemNotificationId(scope.copy(role = UserRole.STAFF), "event-a"))
        assertNotEquals(
            systemNotificationGroupKey(scope),
            systemNotificationGroupKey(scope.copy(userId = "customer-b")),
        )
    }

    @Test
    fun scheduledWorkScope_requiresExactNonGuestIdentity() {
        assertEquals(
            scope,
            notificationScopeFromWorkInput(scope.applicationId, scope.userId, scope.role.name),
        )
        assertNull(notificationScopeFromWorkInput(scope.applicationId, scope.userId, UserRole.GUEST.name))
        assertNull(notificationScopeFromWorkInput(scope.applicationId, "", scope.role.name))
    }

    @Test
    fun firstCatchUpPass_seedsWithoutSelectingHistoricalEvents() {
        val history = listOf(
            OrderHistoryEvent("PENDING", 100L),
            OrderHistoryEvent("CONFIRMED", 200L),
        )

        assertEquals(true, shouldSeedNotificationState(0L))
        assertEquals(emptyList<OrderHistoryEvent>(), notificationHistoryEventsAfter(history, 200L))
    }

    @Test
    fun laterCatchUpPass_selectsOnlyGenuineNewEvents() {
        val history = listOf(
            OrderHistoryEvent("PENDING", 100L),
            OrderHistoryEvent("CONFIRMED", 200L),
        )

        assertEquals(false, shouldSeedNotificationState(100L))
        assertEquals(listOf(history.last()), notificationHistoryEventsAfter(history, 100L))
    }

    @Test
    fun oldAccountWork_cannotRunForNewAccountScope() {
        assertEquals(true, shouldRunNotificationWork(scope, scope))
        assertEquals(false, shouldRunNotificationWork(scope, scope.copy(userId = "customer-b")))
        assertEquals(false, shouldRunNotificationWork(scope, null))
    }

    @Test
    fun eventTimestamps_normalizeSecondsMillisDatesAndNumericStrings() {
        assertEquals(1_700_000_000_000L, notificationEpochMillis(1_700_000_000L))
        assertEquals(1_700_000_000_000L, notificationEpochMillis(1_700_000_000_000L))
        assertEquals(1_700_000_000_000L, notificationEpochMillis(Date(1_700_000_000_000L)))
        assertEquals(1_700_000_000_000L, notificationEpochMillis("1700000000000"))
        assertEquals(0L, notificationEpochMillis("invalid"))
    }

    private companion object {
        val scope = NotificationScope("com.devpro.pizzatime", "customer-a", UserRole.CUSTOMER)
    }
}
