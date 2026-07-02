package com.devpro.pizzatime.core.session

object FakeSessionStore {

    var isLoggedIn: Boolean = false
        private set

    var currentRole: UserRole = UserRole.GUEST
        private set

    fun login(role: UserRole) {
        isLoggedIn = role != UserRole.GUEST
        currentRole = role
    }

    fun logout() {
        isLoggedIn = false
        currentRole = UserRole.GUEST
    }
}

