package com.devpro.pizzatime.feature.staff.navigation

import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.LayoutStaffBottomNavBinding

fun LayoutStaffBottomNavBinding.setupStaffBottomNav(
    currentTab: StaffBottomNavTab,
    onDashboardClick: () -> Unit = {},
    onKitchenClick: () -> Unit = {},
    onDeliveryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    navDashboard.setStaffTabSelected(currentTab == StaffBottomNavTab.DASHBOARD)
    navKitchen.setStaffTabSelected(currentTab == StaffBottomNavTab.KITCHEN)
    navDelivery.setStaffTabSelected(currentTab == StaffBottomNavTab.DELIVERY)
    navProfile.setStaffTabSelected(currentTab == StaffBottomNavTab.PROFILE)

    navDashboard.setOnClickListener {
        if (currentTab != StaffBottomNavTab.DASHBOARD) {
            onDashboardClick()
        }
    }

    navKitchen.setOnClickListener {
        if (currentTab != StaffBottomNavTab.KITCHEN) {
            onKitchenClick()
        }
    }

    navDelivery.setOnClickListener {
        if (currentTab != StaffBottomNavTab.DELIVERY) {
            onDeliveryClick()
        }
    }

    navProfile.setOnClickListener {
        if (currentTab != StaffBottomNavTab.PROFILE) {
            onProfileClick()
        }
    }
}

private fun TextView.setStaffTabSelected(isSelected: Boolean) {
    if (isSelected) {
        setBackgroundResource(R.drawable.bg_bottom_nav_item_selected)
        setTextColor(COLOR_SELECTED)
    } else {
        background = null
        setTextColor(COLOR_UNSELECTED)
    }
}

private val COLOR_SELECTED = "#3A210D".toColorInt()
private val COLOR_UNSELECTED = "#D8C8BC".toColorInt()