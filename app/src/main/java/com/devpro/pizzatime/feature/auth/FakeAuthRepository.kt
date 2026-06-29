package com.devpro.pizzatime.feature.auth

import com.devpro.pizzatime.core.session.UserRole

object FakeAuthRepository {

    fun login(identifier: String, password: String): Result<AuthUserUiModel> {
        val cleanIdentifier = identifier.trim().lowercase()
        val cleanPassword = password.trim()

        if (cleanIdentifier.isBlank() || cleanPassword.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Identifier and password are required"),
            )
        }

        return Result.success(
            AuthUserUiModel(
                identifier = cleanIdentifier,
                displayName = resolveDisplayName(cleanIdentifier),
                role = resolveRole(cleanIdentifier),
            ),
        )
    }

    private fun resolveRole(identifier: String): UserRole {
        return when {
            identifier.contains("admin") -> UserRole.ADMIN
            identifier.contains("staff") -> UserRole.STAFF
            identifier.contains("kitchen") -> UserRole.KITCHEN
            identifier.contains("shipper") -> UserRole.SHIPPER
            identifier.contains("customer") -> UserRole.CUSTOMER
            else -> UserRole.CUSTOMER
        }
    }

    private fun resolveDisplayName(identifier: String): String {
        return when {
            identifier.contains("admin") -> "Admin"
            identifier.contains("staff") -> "Staff"
            identifier.contains("kitchen") -> "Kitchen Staff"
            identifier.contains("shipper") -> "Shipper"
            else -> "Customer"
        }
    }
}