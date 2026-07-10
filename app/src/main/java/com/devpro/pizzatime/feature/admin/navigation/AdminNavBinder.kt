package com.devpro.pizzatime.feature.admin.navigation

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.bindStaffBottomNav
import com.devpro.pizzatime.feature.staff.navigation.bindStaffTopBar
import com.devpro.pizzatime.shared.drawer.StaffDrawerItem

enum class AdminBottomNavDestination {
    DASHBOARD,
    MENU,
    SHIPPER,
    PROFILE,
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
        showEmployeeName = false,
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
    root.findViewById<TextView>(R.id.navDashboard)?.setText(R.string.admin_nav_dashboard)
    root.findViewById<TextView>(R.id.navKitchen)?.setText(R.string.admin_nav_manage_menu)
    root.findViewById<TextView>(R.id.navDelivery)?.setText(R.string.admin_nav_shipper)
    root.findViewById<TextView>(R.id.navProfile)?.setText(R.string.admin_nav_profile)

    bindStaffBottomNav(
        root = root,
        currentTab = selectedDestination.toStaffTab(),
        onDashboardClick = onDashboardClick,
        onKitchenClick = onManageMenuClick,
        onDeliveryClick = onManagePromoCodesClick,
        onProfileClick = onManageStaffClick,
    )
}


private fun AdminBottomNavDestination.toStaffTab(): StaffBottomNavTab {
    return when (this) {
        AdminBottomNavDestination.DASHBOARD -> StaffBottomNavTab.DASHBOARD
        AdminBottomNavDestination.MENU -> StaffBottomNavTab.KITCHEN
        AdminBottomNavDestination.SHIPPER -> StaffBottomNavTab.DELIVERY
        AdminBottomNavDestination.PROFILE -> StaffBottomNavTab.PROFILE
        AdminBottomNavDestination.PROMOS -> StaffBottomNavTab.DASHBOARD
        AdminBottomNavDestination.STAFF -> StaffBottomNavTab.DASHBOARD
    }
}
