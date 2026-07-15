package com.devpro.pizzatime.core.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.devpro.pizzatime.MainActivity
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessage
import com.devpro.pizzatime.core.ui.message.UiMessageDuration
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.UiText

object PizzaTimeNotificationManager {

    private var initialized = false

    fun init(context: Context) {
        if (initialized) {
            return
        }
        ensureChannels(context.applicationContext)
        initialized = true
    }

    @SuppressLint("MissingPermission")
    fun postSystemNotification(
        context: Context,
        notification: AppNotification,
    ): Boolean {
        val appContext = context.applicationContext
        ensureChannels(appContext)
        val scope = NotificationSessionResolver.scopeForNotification(notification) ?: return false
        if (!NotificationPermissionHelper.canPostSystemNotifications(appContext)) {
            Log.d(TAG, "Notification permission disabled")
            return false
        }

        val publicNotification = NotificationCompat.Builder(appContext, channelIdFor(notification.type))
            .setSmallIcon(R.drawable.ic_notification_pizza)
            .setContentTitle(appContext.getString(R.string.notification_public_title))
            .setContentText(appContext.getString(R.string.notification_public_body))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        val builder = NotificationCompat.Builder(appContext, channelIdFor(notification.type))
            .setSmallIcon(R.drawable.ic_notification_pizza)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setWhen(notification.createdAtMillis)
            .setShowWhen(true)
            .setGroup(systemNotificationGroupKey(scope))
            .setContentIntent(buildPendingIntent(appContext, scope, notification))

        return runCatching {
            NotificationManagerCompat.from(appContext)
                .notify(systemNotificationId(scope, notification.dedupeKey), builder.build())
            true
        }.getOrElse { error ->
            Log.w(TAG, "System notification delivery failed", error)
            false
        }
    }

    fun showForegroundMessage(notification: AppNotification) {
        val text = UiText.Dynamic.from(notification.title) ?: return
        AppUiMessageBus.publish(
            UiMessage(
                text = text,
                type = UiMessageType.INFO,
                duration = UiMessageDuration.LONG,
            ),
        )
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(ORDER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ORDER_CHANNEL_ID,
                context.getString(R.string.notification_channel_orders_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_orders_description)
                enableVibration(true)
                setShowBadge(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null,
                )
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            manager.createNotificationChannel(channel)
        }

        if (manager.getNotificationChannel(REVIEW_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                REVIEW_CHANNEL_ID,
                context.getString(R.string.notification_channel_reviews_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_reviews_description)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildPendingIntent(
        context: Context,
        scope: NotificationScope,
        notification: AppNotification,
    ): PendingIntent {
        val requestCode = systemNotificationId(scope, notification.dedupeKey)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.Builder()
                .scheme("pizzatime-notification")
                .authority("open")
                .appendPath(systemNotificationIntentToken(scope, notification.dedupeKey))
                .build()
            putExtra(
                NotificationDeepLinkContract.EXTRA_NOTIFICATION_DEEP_LINK,
                notification.deepLinkType.name,
            )
            putExtra(NotificationDeepLinkContract.EXTRA_ORDER_ID, notification.orderId)
            putExtra(NotificationDeepLinkContract.EXTRA_REVIEW_ID, notification.reviewId)
            putExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_APPLICATION_ID, scope.applicationId)
            putExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_RECIPIENT_USER_ID, scope.userId)
            putExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_RECIPIENT_ROLE, scope.role.name)
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun channelIdFor(type: NotificationType): String {
        return when (type) {
            NotificationType.ADMIN_ORDER_REVIEW,
            NotificationType.ADMIN_PRODUCT_REVIEW,
            -> REVIEW_CHANNEL_ID

            else -> ORDER_CHANNEL_ID
        }
    }

    private const val ORDER_CHANNEL_ID = "pizzatime_order_updates"
    private const val REVIEW_CHANNEL_ID = "pizzatime_reviews"
    private const val TAG = "NotificationDispatch"
}
