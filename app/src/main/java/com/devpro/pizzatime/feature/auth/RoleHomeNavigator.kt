package com.devpro.pizzatime.feature.auth

import androidx.fragment.app.Fragment
import com.devpro.pizzatime.core.notification.OrderNotificationMonitor
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.clearAppBackStack
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.openStaffDashboard

fun Fragment.restoreSessionAndOpenRoleHome(user: AuthUserUiModel): Boolean {
    FakeSessionStore.login(user.role)
    CartStore.onUserChanged(user.uid)
    return openRoleHome(user.role)
}

fun Fragment.openRoleHome(role: UserRole): Boolean {
    if (role == UserRole.GUEST) {
        return false
    }

    OrderNotificationMonitor.start(role)
    clearAppBackStack()
    when (role) {
        UserRole.CUSTOMER -> openCustomerHome(addToBackStack = false, animate = false)
        UserRole.STAFF -> openStaffDashboard(addToBackStack = false, animate = false)
        UserRole.KITCHEN -> openKitchenBoard(addToBackStack = false, animate = false)
        UserRole.SHIPPER -> openShipperDeliveryDashboard(addToBackStack = false, animate = false)
        UserRole.ADMIN -> openAdminDashboard(addToBackStack = false, animate = false)
        UserRole.GUEST -> return false
    }
    return true
}
