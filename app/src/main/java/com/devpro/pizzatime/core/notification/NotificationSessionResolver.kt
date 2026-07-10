package com.devpro.pizzatime.core.notification

import android.content.Context
import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.session.UserRole
import com.google.firebase.auth.FirebaseAuth

object NotificationSessionResolver {

    fun currentRole(): UserRole {
        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (userId.isBlank()) {
            return UserRole.GUEST
        }

        return when (AppEditionConfig.current) {
            AppEdition.GUEST,
            AppEdition.CUSTOMER,
            -> UserRole.CUSTOMER

            AppEdition.STAFF -> UserRole.STAFF
            AppEdition.KITCHEN -> UserRole.KITCHEN
            AppEdition.SHIPPER -> UserRole.SHIPPER
            AppEdition.ADMIN -> UserRole.ADMIN
        }
    }

    fun currentScope(context: Context): NotificationScope? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty().trim()
        if (userId.isBlank()) {
            return null
        }

        val role = currentRole()
        if (role == UserRole.GUEST) {
            return null
        }

        return NotificationScope(
            applicationId = context.packageName,
            userId = userId,
            role = role,
        )
    }

    fun scopeForNotification(
        context: Context,
        notification: AppNotification,
    ): NotificationScope? {
        val userId = notification.recipientUserId?.trim().orEmpty()
        if (userId.isBlank()) {
            return currentScope(context)
        }

        return NotificationScope(
            applicationId = context.packageName,
            userId = userId,
            role = notification.recipientRole,
        )
    }
}
