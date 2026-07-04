package com.devpro.pizzatime.feature.staff.navigation

import android.view.View
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.shared.drawer.StaffDrawerItem

fun Fragment.bindStaffTopBar(
    root: View,
    title: CharSequence? = null,
    selectedDrawerItem: StaffDrawerItem = StaffDrawerItem.STAFF_SCHEDULE,
    onMenuClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
) {
    root.findViewById<TextView>(R.id.tvAdminTitle)?.let { titleView ->
        if (title != null) {
            titleView.text = title
        }
    }

    root.findViewById<View>(R.id.btnMenu)?.setOnClickListener {
        onMenuClick?.invoke() ?: openStaffDrawer(selectedDrawerItem)
    }

    root.findViewById<View>(R.id.tvAdminAvatar)?.setOnClickListener {
        onAvatarClick?.invoke() ?: openStaffDrawer(selectedDrawerItem)
    }
}

fun Fragment.bindStaffBottomNav(
    root: View,
    currentTab: StaffBottomNavTab,
    onDashboardClick: (() -> Unit)? = null,
    onKitchenClick: (() -> Unit)? = null,
    onDeliveryClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
) {
    root.findViewById<TextView>(R.id.navDashboard)?.bindStaffNavItem(
        selected = currentTab == StaffBottomNavTab.DASHBOARD,
        onClick = {
            if (currentTab != StaffBottomNavTab.DASHBOARD) {
                onDashboardClick?.invoke() ?: openStaffDashboard()
            }
        },
    )

    root.findViewById<TextView>(R.id.navKitchen)?.bindStaffNavItem(
        selected = currentTab == StaffBottomNavTab.KITCHEN,
        onClick = {
            if (currentTab != StaffBottomNavTab.KITCHEN) {
                onKitchenClick?.invoke() ?: openKitchenBoard()
            }
        },
    )

    root.findViewById<TextView>(R.id.navDelivery)?.bindStaffNavItem(
        selected = currentTab == StaffBottomNavTab.DELIVERY,
        onClick = {
            if (currentTab != StaffBottomNavTab.DELIVERY) {
                onDeliveryClick?.invoke() ?: openShipperDeliveryDashboard()
            }
        },
    )

    root.findViewById<TextView>(R.id.navProfile)?.bindStaffNavItem(
        selected = currentTab == StaffBottomNavTab.PROFILE,
        onClick = {
            if (currentTab != StaffBottomNavTab.PROFILE) {
                onProfileClick?.invoke() ?: openCustomerAccount()
            }
        },
    )
}

private fun TextView.bindStaffNavItem(
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        setBackgroundResource(R.drawable.bg_bottom_nav_item_selected)
        setTextColor(COLOR_SELECTED)
    } else {
        background = null
        setTextColor(COLOR_UNSELECTED)
    }
    setOnClickListener { onClick() }
}

private val COLOR_SELECTED = "#3A210D".toColorInt()
private val COLOR_UNSELECTED = "#D8C8BC".toColorInt()
