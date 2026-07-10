package com.devpro.pizzatime.feature.customer.common.navigation

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.menubottomsheet.CustomerMenuBottomSheetDialog
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderHistory
import com.devpro.pizzatime.feature.staff.navigation.openCustomerPromoCodes
import com.devpro.pizzatime.feature.staff.navigation.openLoginRequiredScreen
import com.devpro.pizzatime.feature.staff.navigation.NavigationDirection

fun Fragment.bindCustomerTopBar(
    root: View,
    cartItemCount: Int = 0,
    onMenuClick: (() -> Unit)? = null,
    onCartClick: (() -> Unit)? = null,
) {
    root.findViewById<View>(R.id.btnMenu)?.setOnClickListener {
        if (onMenuClick != null) {
            onMenuClick()
        } else {
            CustomerMenuBottomSheetDialog.show(parentFragmentManager)
        }
    }

    root.findViewById<View>(R.id.cartButtonContainer)?.setOnClickListener {
        if (onCartClick != null) {
            onCartClick()
        } else {
            openCartScreen()
        }
    }

    root.findViewById<TextView>(R.id.tvCartBadge)?.bindCartBadge(cartItemCount)
}

fun Fragment.bindCustomerBottomNav(
    root: View,
    selectedTab: CustomerBottomNavTab,
    onCustomerMenuClick: (() -> Unit)? = null,
    onCustomerOrdersClick: (() -> Unit)? = null,
    onCustomerLoyaltyClick: (() -> Unit)? = null,
    onCustomerProfileClick: (() -> Unit)? = null,
) {
    root.findViewById<TextView>(R.id.navMenu)?.bindCustomerNavItem(
        selected = selectedTab == CustomerBottomNavTab.MENU,
        onClick = {
            if (selectedTab != CustomerBottomNavTab.MENU) {
                onCustomerMenuClick?.invoke()
                    ?: openCustomerHome(
                        addToBackStack = false,
                        direction = selectedTab.directionTo(CustomerBottomNavTab.MENU),
                    )
            }
        },
    )

    root.findViewById<TextView>(R.id.navOrders)?.bindCustomerNavItem(
        selected = selectedTab == CustomerBottomNavTab.ORDERS,
        onClick = {
            if (selectedTab != CustomerBottomNavTab.ORDERS) {
                onCustomerOrdersClick?.invoke()
                    ?: openCustomerOrdersOrLogin(selectedTab.directionTo(CustomerBottomNavTab.ORDERS))
            }
        },
    )

    root.findViewById<TextView>(R.id.navLoyalty)?.bindCustomerNavItem(
        selected = selectedTab == CustomerBottomNavTab.LOYALTY,
        onClick = {
            if (selectedTab != CustomerBottomNavTab.LOYALTY) {
                onCustomerLoyaltyClick?.invoke()
                    ?: openCustomerPromoCodes(
                        addToBackStack = false,
                        direction = selectedTab.directionTo(CustomerBottomNavTab.LOYALTY),
                    )
            }
        },
    )

    root.findViewById<TextView>(R.id.navProfile)?.bindCustomerNavItem(
        selected = selectedTab == CustomerBottomNavTab.PROFILE,
        onClick = {
            if (selectedTab != CustomerBottomNavTab.PROFILE) {
                onCustomerProfileClick?.invoke()
                    ?: openCustomerProfileOrLogin(selectedTab.directionTo(CustomerBottomNavTab.PROFILE))
            }
        },
    )
}

private fun Fragment.openCustomerOrdersOrLogin(direction: NavigationDirection) {
    if (FakeSessionStore.isLoggedIn) {
        openCustomerOrderHistory(addToBackStack = false, direction = direction)
    } else {
        openLoginRequiredScreen()
    }
}

private fun Fragment.openCustomerProfileOrLogin(direction: NavigationDirection) {
    if (FakeSessionStore.isLoggedIn) {
        openCustomerAccount(addToBackStack = false, direction = direction)
    } else {
        openLoginRequiredScreen()
    }
}

private fun CustomerBottomNavTab.directionTo(target: CustomerBottomNavTab): NavigationDirection {
    return if (target.ordinal > ordinal) NavigationDirection.FORWARD else NavigationDirection.BACKWARD
}

private fun TextView.bindCustomerNavItem(
    selected: Boolean,
    onClick: () -> Unit,
) {
    setBackgroundResourceIfSelected(selected)
    setTextColor(
        ContextCompat.getColor(
            context,
            if (selected) R.color.pt_text_dark else R.color.pt_text_primary,
        ),
    )
    setOnClickListener { onClick() }
}

private fun TextView.setBackgroundResourceIfSelected(selected: Boolean) {
    if (selected) {
        setBackgroundResource(R.drawable.bg_bottom_nav_item_selected)
    } else {
        setBackgroundColor(Color.TRANSPARENT)
    }
}

private fun TextView.bindCartBadge(cartItemCount: Int) {
    isVisible = cartItemCount > 0
    text = when {
        cartItemCount <= 0 -> ""
        cartItemCount > MAX_BADGE_COUNT -> MAX_BADGE_TEXT
        else -> cartItemCount.toString()
    }
}

private const val MAX_BADGE_COUNT = 99
private const val MAX_BADGE_TEXT = "99+"
