package com.devpro.pizzatime.feature.staff.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.admin.dashboard.AdminDashboardFragment
import com.devpro.pizzatime.feature.admin.menu.ManageMenuFragment
import com.devpro.pizzatime.feature.admin.promo.ManagePromoCodesFragment
import com.devpro.pizzatime.feature.admin.reports.ReportsFragment
import com.devpro.pizzatime.feature.admin.staff.ManageStaffFragment
import com.devpro.pizzatime.feature.kitchen.board.KitchenBoardFragment
import com.devpro.pizzatime.feature.shipper.dashboard.ShipperDeliveryDashboardFragment
import com.devpro.pizzatime.feature.shipper.detail.ShipperDeliveryDetailFragment
import com.devpro.pizzatime.feature.staff.dashboard.StaffDashboardFragment
import com.devpro.pizzatime.feature.staff.detail.StaffOrderDetailFragment

fun Fragment.openStaffDashboard(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = StaffDashboardFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openKitchenBoard(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = KitchenBoardFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openShipperDeliveryDashboard(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ShipperDeliveryDashboardFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openStaffOrderDetail(orderId: String) {
    replaceStaffFlowFragment(
        fragment = StaffOrderDetailFragment.newInstance(orderId),
        addToBackStack = true,
    )
}

fun Fragment.openShipperDeliveryDetail(orderId: String) {
    replaceStaffFlowFragment(
        fragment = ShipperDeliveryDetailFragment.newInstance(orderId),
        addToBackStack = true,
    )
}

fun Fragment.openAdminDashboard(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = AdminDashboardFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openManageMenu(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ManageMenuFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openManagePromoCodes(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ManagePromoCodesFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openManageStaff(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ManageStaffFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openReports(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ReportsFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.backToPreviousStaffScreen() {
    parentFragmentManager.popBackStack()
}

private fun Fragment.replaceStaffFlowFragment(
    fragment: Fragment,
    addToBackStack: Boolean,
) {
    parentFragmentManager.beginTransaction()
        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        .replace(R.id.fragmentContainer, fragment)
        .applyBackStack(addToBackStack)
        .commit()
}

private fun FragmentTransaction.applyBackStack(
    addToBackStack: Boolean,
): FragmentTransaction {
    return if (addToBackStack) {
        addToBackStack(null)
    } else {
        this
    }
}