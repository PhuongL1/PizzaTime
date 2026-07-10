package com.devpro.pizzatime.feature.customer.account

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.directionTo
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.openStaffDashboard

fun Fragment.bindRoleAwareAccountBottomNav(
    root: View,
    role: UserRole,
) {
    when (role) {
        UserRole.CUSTOMER -> bindCustomerBottomNav(
            root = root,
            selectedTab = CustomerBottomNavTab.PROFILE,
        )

        UserRole.ADMIN -> bindFourSlotAccountNav(
            root = root,
            labels = AccountNavLabels(
                first = getString(R.string.admin_nav_dashboard),
                second = getString(R.string.admin_nav_manage_menu),
                third = getString(R.string.admin_nav_shipper),
                fourth = getString(R.string.admin_nav_profile),
            ),
            onFirstClick = { openAdminDashboard() },
            onSecondClick = { openManageMenu() },
            onThirdClick = { openShipperDeliveryDashboard() },
        )

        UserRole.STAFF,
        UserRole.KITCHEN,
        UserRole.SHIPPER,
            -> bindFourSlotAccountNav(
                root = root,
                labels = AccountNavLabels(
                    first = getString(R.string.operations_nav_staff),
                    second = getString(R.string.operations_nav_kitchen),
                    third = getString(R.string.operations_nav_shipper),
                    fourth = getString(R.string.operations_nav_profile),
                ),
                onFirstClick = {
                    openStaffDashboard(direction = StaffBottomNavTab.PROFILE.directionTo(StaffBottomNavTab.DASHBOARD))
                },
                onSecondClick = {
                    openKitchenBoard(direction = StaffBottomNavTab.PROFILE.directionTo(StaffBottomNavTab.KITCHEN))
                },
                onThirdClick = {
                    openShipperDeliveryDashboard(
                        direction = StaffBottomNavTab.PROFILE.directionTo(StaffBottomNavTab.DELIVERY),
                    )
                },
            )

        UserRole.GUEST -> root.visibility = View.GONE
    }
}

private fun Fragment.bindFourSlotAccountNav(
    root: View,
    labels: AccountNavLabels,
    onFirstClick: () -> Unit,
    onSecondClick: () -> Unit,
    onThirdClick: () -> Unit,
) {
    root.visibility = View.VISIBLE
    root.findViewById<TextView>(R.id.navMenu)?.bindAccountNavItem(
        label = labels.first,
        selected = false,
        onClick = onFirstClick,
    )
    root.findViewById<TextView>(R.id.navOrders)?.bindAccountNavItem(
        label = labels.second,
        selected = false,
        onClick = onSecondClick,
    )
    root.findViewById<TextView>(R.id.navLoyalty)?.bindAccountNavItem(
        label = labels.third,
        selected = false,
        onClick = onThirdClick,
    )
    root.findViewById<TextView>(R.id.navProfile)?.bindAccountNavItem(
        label = labels.fourth,
        selected = true,
        onClick = {},
    )
}

private fun TextView.bindAccountNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    text = label
    if (selected) {
        setBackgroundResource(R.drawable.bg_bottom_nav_item_selected)
        setTextColor(ContextCompat.getColor(context, R.color.pt_text_dark))
    } else {
        setBackgroundColor(Color.TRANSPARENT)
        setTextColor(ContextCompat.getColor(context, R.color.pt_text_primary))
    }
    setOnClickListener {
        if (!selected) {
            onClick()
        }
    }
}

private data class AccountNavLabels(
    val first: String,
    val second: String,
    val third: String,
    val fourth: String,
)
