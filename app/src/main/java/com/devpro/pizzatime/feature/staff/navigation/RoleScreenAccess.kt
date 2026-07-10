package com.devpro.pizzatime.feature.staff.navigation

import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole

fun currentAppRole(): UserRole = FakeSessionStore.currentRole

fun canManageStaffScreen(): Boolean {
    return currentAppRole() in setOf(UserRole.STAFF, UserRole.ADMIN)
}

fun canManageKitchenScreen(): Boolean {
    return currentAppRole() in setOf(UserRole.KITCHEN, UserRole.ADMIN)
}

fun canManageShipperScreen(): Boolean {
    return currentAppRole() in setOf(UserRole.SHIPPER, UserRole.ADMIN)
}
