package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.core.session.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationDeliveryProcessorTest {

    @Test
    fun foregroundEvent_persistsOnceAndDeliversOnlyUiMessage() {
        val gateway = FakeGateway()
        val processor = NotificationDeliveryProcessor(gateway)

        val result = processor.process(scope, notification(), isForeground = true)

        assertEquals(NotificationProcessingResult.FOREGROUND_DELIVERED, result)
        assertEquals(1, gateway.persistCount)
        assertEquals(1, gateway.foregroundCount)
        assertEquals(0, gateway.systemCount)
    }

    @Test
    fun backgroundEvent_persistsOnceAndPostsOnlySystemNotification() {
        val gateway = FakeGateway()
        val processor = NotificationDeliveryProcessor(gateway)

        val result = processor.process(scope, notification(), isForeground = false)

        assertEquals(NotificationProcessingResult.BACKGROUND_POSTED, result)
        assertEquals(1, gateway.persistCount)
        assertEquals(0, gateway.foregroundCount)
        assertEquals(1, gateway.systemCount)
    }

    @Test
    fun permissionDenied_keepsInboxAndSuppressesOnlySystemDelivery() {
        val gateway = FakeGateway(systemPostSucceeds = false)
        val processor = NotificationDeliveryProcessor(gateway)

        val result = processor.process(scope, notification(), isForeground = false)

        assertEquals(NotificationProcessingResult.BACKGROUND_SUPPRESSED, result)
        assertEquals(1, gateway.persistCount)
        assertEquals(1, gateway.systemCount)
    }

    @Test
    fun sameFirestoreAndFcmEvent_isProcessedOnce() {
        val gateway = FakeGateway()
        val processor = NotificationDeliveryProcessor(gateway)
        val event = notification()

        processor.process(scope, event, isForeground = true)
        val duplicateResult = processor.process(scope, event, isForeground = false)

        assertEquals(NotificationProcessingResult.DUPLICATE, duplicateResult)
        assertEquals(1, gateway.persistCount)
        assertEquals(1, gateway.foregroundCount)
        assertEquals(0, gateway.systemCount)
    }

    @Test
    fun sameWorkerAndFirestoreEvent_isProcessedOnce() {
        val gateway = FakeGateway()
        val processor = NotificationDeliveryProcessor(gateway)

        processor.process(scope, notification(), isForeground = false)
        processor.process(scope, notification(), isForeground = false)

        assertEquals(1, gateway.persistCount)
        assertEquals(1, gateway.systemCount)
    }

    @Test
    fun eventForAnotherAccount_isRejectedBeforePersistence() {
        val gateway = FakeGateway()
        val processor = NotificationDeliveryProcessor(gateway)

        val result = processor.process(
            scope = scope,
            notification = notification().copy(recipientUserId = "customer-b"),
            isForeground = true,
        )

        assertEquals(NotificationProcessingResult.REJECTED, result)
        assertEquals(0, gateway.persistCount)
    }

    @Test
    fun failedInboxPersistence_doesNotRecordOrDeliverEvent() {
        val gateway = FakeGateway(persistSucceeds = false)
        val processor = NotificationDeliveryProcessor(gateway)

        val result = processor.process(scope, notification(), isForeground = true)

        assertEquals(NotificationProcessingResult.PERSISTENCE_FAILED, result)
        assertEquals(0, gateway.foregroundCount)
        assertEquals(0, gateway.systemCount)
    }

    private class FakeGateway(
        private val systemPostSucceeds: Boolean = true,
        private val persistSucceeds: Boolean = true,
    ) : NotificationDeliveryGateway {
        private val processedKeys = mutableSetOf<String>()
        private val persistedKeys = mutableSetOf<String>()
        var persistCount = 0
        var foregroundCount = 0
        var systemCount = 0

        override fun isDuplicate(scope: NotificationScope, notification: AppNotification): Boolean {
            return notification.dedupeKey in processedKeys || notification.dedupeKey in persistedKeys
        }

        override fun persist(scope: NotificationScope, notification: AppNotification): Boolean {
            persistCount += 1
            if (!persistSucceeds) return false
            persistedKeys += notification.dedupeKey
            return true
        }

        override fun recordProcessed(scope: NotificationScope, dedupeKey: String) {
            processedKeys += dedupeKey
        }

        override fun deliverForeground(notification: AppNotification) {
            foregroundCount += 1
        }

        override fun deliverSystem(notification: AppNotification): Boolean {
            systemCount += 1
            return systemPostSucceeds
        }
    }

    private companion object {
        val scope = NotificationScope(
            applicationId = "com.devpro.pizzatime",
            userId = "customer-a",
            role = UserRole.CUSTOMER,
        )

        fun notification(): AppNotification {
            return AppNotification(
                id = "order:order-a:status:CONFIRMED:1000",
                dedupeKey = "order:order-a:status:CONFIRMED:1000",
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
}
