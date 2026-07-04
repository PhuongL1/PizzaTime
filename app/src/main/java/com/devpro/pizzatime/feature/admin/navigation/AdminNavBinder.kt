package com.devpro.pizzatime.feature.admin.navigation

import android.view.View
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.bindStaffBottomNav
import com.devpro.pizzatime.feature.staff.navigation.bindStaffTopBar
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openManagePromoCodes
import com.devpro.pizzatime.feature.staff.navigation.openManageStaff
import com.devpro.pizzatime.shared.drawer.StaffDrawerItem

enum class AdminBottomNavDestination {
    DASHBOARD,
    MENU,
    PROMOS,
    STAFF,
}

fun Fragment.bindAdminTopBar(
    root: View,
    title: CharSequence? = null,
    selectedDrawerItem: StaffDrawerItem = StaffDrawerItem.STAFF_SCHEDULE,
    onMenuClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
) {
    bindStaffTopBar(
        root = root,
        title = title ?: getString(R.string.staff_dashboard_title),
        selectedDrawerItem = selectedDrawerItem,
        onMenuClick = onMenuClick,
        onAvatarClick = onAvatarClick,
    )
}

fun Fragment.bindAdminBottomNav(
    root: View,
    selectedDestination: AdminBottomNavDestination,
    onDashboardClick: (() -> Unit)? = null,
    onManageMenuClick: (() -> Unit)? = null,
    onManagePromoCodesClick: (() -> Unit)? = null,
    onManageStaffClick: (() -> Unit)? = null,
) {
    // Admin screens currently reuse layout_staff_bottom_nav.xml. This maps the
    // four existing slots to admin destinations without changing the layout.
    bindStaffBottomNav(
        root = root,
        currentTab = selectedDestination.toStaffTab(),
        onDashboardClick = onDashboardClick ?: { openAdminDashboard() },
        onKitchenClick = onManageMenuClick ?: { openManageMenu() },
        onDeliveryClick = onManagePromoCodesClick ?: { openManagePromoCodes() },
        onProfileClick = onManageStaffClick ?: { openManageStaff() },
    )
}

private fun AdminBottomNavDestination.toStaffTab(): StaffBottomNavTab {
    return when (this) {
        AdminBottomNavDestination.DASHBOARD -> StaffBottomNavTab.DASHBOARD
        AdminBottomNavDestination.MENU -> StaffBottomNavTab.KITCHEN
        AdminBottomNavDestination.PROMOS -> StaffBottomNavTab.DELIVERY
        AdminBottomNavDestination.STAFF -> StaffBottomNavTab.PROFILE
    }
}
