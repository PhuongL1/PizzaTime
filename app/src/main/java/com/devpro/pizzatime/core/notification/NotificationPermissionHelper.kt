package com.devpro.pizzatime.core.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {

    fun requiresRuntimePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return !requiresRuntimePermission() ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun canPostSystemNotifications(context: Context): Boolean {
        return hasNotificationPermission(context) && areNotificationsEnabled(context)
    }

    fun shouldRequestNotificationPermission(context: Context): Boolean {
        return requiresRuntimePermission() && !hasNotificationPermission(context)
    }
}
