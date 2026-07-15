package com.devpro.pizzatime.core.notification

import android.content.Context
import android.util.Log

object NotificationDispatcher {

    private val dispatchLock = Any()

    fun dispatch(
        context: Context,
        notification: AppNotification,
        sourceTag: String,
    ) {
        val scope = NotificationSessionResolver.scopeForNotification(notification) ?: return
        val appContext = context.applicationContext
        val result = synchronized(dispatchLock) {
            NotificationDeliveryProcessor(
                gateway = androidGateway(appContext),
            ).process(
                scope = scope,
                notification = notification,
                isForeground = AppForegroundState.isForeground,
            )
        }
        when (result) {
            NotificationProcessingResult.REJECTED -> Log.w(TAG, "Rejected source=$sourceTag")
            NotificationProcessingResult.DUPLICATE -> Log.d(TAG, "Duplicate skipped source=$sourceTag")
            NotificationProcessingResult.PERSISTENCE_FAILED -> Log.w(
                TAG,
                "Inbox persistence rejected source=$sourceTag",
            )
            NotificationProcessingResult.FOREGROUND_DELIVERED -> Log.d(TAG, "Foreground UI delivery source=$sourceTag")
            NotificationProcessingResult.BACKGROUND_POSTED -> Log.d(TAG, "Background system delivery source=$sourceTag")
            NotificationProcessingResult.BACKGROUND_SUPPRESSED -> Log.d(
                TAG,
                "Background delivery suppressed source=$sourceTag",
            )
        }
    }

    private fun androidGateway(context: Context): NotificationDeliveryGateway {
        return object : NotificationDeliveryGateway {
            override fun isDuplicate(
                scope: NotificationScope,
                notification: AppNotification,
            ): Boolean {
                return NotificationStateStore.hasDedupeKey(scope, notification.dedupeKey) ||
                    NotificationInboxStore.containsDedupeKey(scope, notification.dedupeKey)
            }

            override fun persist(
                scope: NotificationScope,
                notification: AppNotification,
            ): Boolean {
                return NotificationSessionResolver.currentScope() == scope &&
                    NotificationInboxStore.addOrUpdate(notification)
            }

            override fun recordProcessed(
                scope: NotificationScope,
                dedupeKey: String,
            ) {
                NotificationStateStore.recordDedupeKey(scope, dedupeKey)
            }

            override fun deliverForeground(notification: AppNotification) {
                PizzaTimeNotificationManager.showForegroundMessage(notification)
            }

            override fun deliverSystem(notification: AppNotification): Boolean {
                return PizzaTimeNotificationManager.postSystemNotification(context, notification)
            }
        }
    }

    private const val TAG = "NotificationDispatch"
}
