package com.devpro.pizzatime.core.notification

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationWorkScheduler {

    fun schedule(
        context: Context,
        scope: NotificationScope,
    ) {
        val request = PeriodicWorkRequestBuilder<NotificationCatchUpWorker>(
            15,
            TimeUnit.MINUTES,
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        ).build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                uniqueWorkName(scope),
                ExistingPeriodicWorkPolicy.REPLACE,
                request,
            )
        Log.d(TAG, "Scheduled worker role=${scope.role.name}")
    }

    fun cancel(
        context: Context,
        scope: NotificationScope,
    ) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(uniqueWorkName(scope))
        Log.d(TAG, "Cancelled worker role=${scope.role.name}")
    }

    private fun uniqueWorkName(scope: NotificationScope): String {
        return "notification_catch_up_${scope.applicationId}_${scope.userId}_${scope.role.name.lowercase()}"
    }

    private const val TAG = "NotificationWorker"
}
