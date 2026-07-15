package com.devpro.pizzatime.core.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@Suppress("DEPRECATION")
class PizzaTimeFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenRegistrar.saveTokenForCurrentUser(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.notification != null) {
            Log.w(TAG, "Notification payload rejected; data-only payload required")
            return
        }
        AppForegroundState.init()
        NotificationInboxStore.init(applicationContext)
        NotificationStateStore.init(applicationContext)
        PizzaTimeNotificationManager.init(applicationContext)
        val scope = NotificationSessionResolver.currentScope() ?: return
        val notification = NotificationEventFactory.createFcmNotification(
            context = applicationContext,
            scope = scope,
            data = message.data,
        ) ?: run {
            Log.w(TAG, "Invalid data payload rejected")
            return
        }
        NotificationDispatcher.dispatch(applicationContext, notification, TAG)
    }

    companion object {
        private const val TAG = "PizzaTimeFCM"
    }
}
