package com.devpro.pizzatime.feature.auth

import com.devpro.pizzatime.core.session.UserRole

data class AuthUserUiModel(
    val uid: String,
    val identifier: String,
    val displayName: String,
    val role: UserRole,
)
