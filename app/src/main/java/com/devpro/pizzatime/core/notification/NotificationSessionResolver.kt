package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.BuildConfig
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

    fun currentScope(): NotificationScope? {
        if (AppEditionConfig.isGuestEdition) {
            return null
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty().trim()
        if (userId.isBlank()) {
            return null
        }

        val role = currentRole()
        if (role == UserRole.GUEST) {
            return null
        }

        return NotificationScope(
            applicationId = BuildConfig.APPLICATION_ID,
            userId = userId,
            role = role,
        )
    }

    fun scopeForNotification(
        notification: AppNotification,
    ): NotificationScope? {
        if (AppEditionConfig.isGuestEdition) {
            return null
        }
        val userId = notification.recipientUserId?.trim().orEmpty()
        if (userId.isBlank()) {
            return currentScope()
        }

        return NotificationScope(
            applicationId = BuildConfig.APPLICATION_ID,
            userId = userId,
            role = notification.recipientRole,
        )
    }
}
