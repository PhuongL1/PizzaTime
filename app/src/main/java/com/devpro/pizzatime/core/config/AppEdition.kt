package com.devpro.pizzatime.core.config

import com.devpro.pizzatime.BuildConfig
import com.devpro.pizzatime.core.session.UserRole

enum class AppEdition {
    GUEST,
    CUSTOMER,
    STAFF,
    KITCHEN,
    SHIPPER,
    ADMIN,
}

object AppEditionConfig {

    val current: AppEdition by lazy {
        runCatching {
            AppEdition.valueOf(BuildConfig.APP_EDITION)
        }.getOrDefault(AppEdition.CUSTOMER)
    }

    val requiredRole: String?
        get() = when (current) {
            AppEdition.GUEST -> null
            AppEdition.CUSTOMER -> "CUSTOMER"
            AppEdition.STAFF -> "STAFF"
            AppEdition.KITCHEN -> "KITCHEN"
            AppEdition.SHIPPER -> "SHIPPER"
            AppEdition.ADMIN -> "ADMIN"
        }

    val isGuestEdition: Boolean
        get() = current == AppEdition.GUEST

    fun isAllowedAuthRole(role: UserRole): Boolean {
        return when (current) {
            AppEdition.GUEST,
            AppEdition.CUSTOMER,
            -> role == UserRole.CUSTOMER

            AppEdition.STAFF -> role == UserRole.STAFF
            AppEdition.KITCHEN -> role == UserRole.KITCHEN
            AppEdition.SHIPPER -> role == UserRole.SHIPPER
            AppEdition.ADMIN -> role == UserRole.ADMIN
        }
    }

    fun editionMismatchMessage(): String {
        return "This account cannot use this app edition."
    }
}
