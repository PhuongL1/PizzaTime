package com.devpro.pizzatime.core.notification

import android.content.Context
import android.util.Log

object NotificationDispatcher {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun dispatch(
        notification: AppNotification,
        sourceTag: String,
    ) {
        val context = contextOrNull() ?: return
        val scope = NotificationSessionResolver.scopeForNotification(context, notification) ?: return
        if (NotificationStateStore.hasDedupeKey(scope, notification.dedupeKey)) {
            Log.d(TAG, "Dedupe skipped source=$sourceTag key=${notification.dedupeKey}")
            return
        }

        NotificationInboxStore.addOrUpdate(notification)
        NotificationStateStore.recordDedupeKey(scope, notification.dedupeKey)
        NotificationEventBus.publish(notification)

        if (AppForegroundState.isForeground) {
            PizzaTimeNotificationManager.showForegroundMessage(context, notification)
            Log.d(TAG, "Foreground event source=$sourceTag id=${notification.id}")
            return
        }

        val posted = PizzaTimeNotificationManager.postSystemNotification(context, notification)
        if (posted) {
            Log.d(TAG, "System notification posted source=$sourceTag id=${notification.id}")
        }
    }

    private fun contextOrNull(): Context? {
        return if (::appContext.isInitialized) appContext else null
    }

    private const val TAG = "NotificationDispatch"
}
