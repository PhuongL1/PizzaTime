package com.devpro.pizzatime.core.notification

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.devpro.pizzatime.core.session.UserRole
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
        ).setInputData(
            workDataOf(
                INPUT_APPLICATION_ID to scope.applicationId,
                INPUT_USER_ID to scope.userId,
                INPUT_ROLE to scope.role.name,
            ),
        ).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            30,
            TimeUnit.SECONDS,
        ).build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                uniqueWorkName(scope),
                ExistingPeriodicWorkPolicy.UPDATE,
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

    internal fun uniqueWorkName(scope: NotificationScope): String {
        return "notification_catch_up_${scope.applicationId}_${scope.userId}_${scope.role.name.lowercase()}"
    }

    internal const val INPUT_APPLICATION_ID = "notification_application_id"
    internal const val INPUT_USER_ID = "notification_user_id"
    internal const val INPUT_ROLE = "notification_role"
    private const val TAG = "NotificationWorker"
}

internal fun notificationScopeFromWorkInput(
    applicationId: String?,
    userId: String?,
    roleName: String?,
): NotificationScope? {
    val resolvedApplicationId = applicationId.orEmpty().trim().takeIf { it.isNotBlank() } ?: return null
    val resolvedUserId = userId.orEmpty().trim().takeIf { it.isNotBlank() } ?: return null
    val role = runCatching { UserRole.valueOf(roleName.orEmpty().trim()) }.getOrNull() ?: return null
    if (role == UserRole.GUEST) return null
    return NotificationScope(
        applicationId = resolvedApplicationId,
        userId = resolvedUserId,
        role = role,
    )
}
