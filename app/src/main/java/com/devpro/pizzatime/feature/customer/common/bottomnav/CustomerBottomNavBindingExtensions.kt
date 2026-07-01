package com.devpro.pizzatime.feature.customer.common.bottomnav

import android.graphics.Color
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.LayoutCustomerBottomNavBinding

fun Fragment.setupCustomerBottomNav(
    bottomNav: LayoutCustomerBottomNavBinding,
    selectedTab: CustomerBottomNavTab,
    onCustomerMenuClick: () -> Unit = {},
    onCustomerOrdersClick: () -> Unit = {},
    onCustomerLoyaltyClick: () -> Unit = {},
    onCustomerProfileClick: () -> Unit = {},
) {
    bottomNav.renderCustomerBottomNav(selectedTab)

    bottomNav.navMenu.setOnClickListener {
        if (selectedTab != CustomerBottomNavTab.MENU) {
            onCustomerMenuClick()
        }
    }

    bottomNav.navOrders.setOnClickListener {
        if (selectedTab != CustomerBottomNavTab.ORDERS) {
            onCustomerOrdersClick()
        }
    }

    bottomNav.navLoyalty.setOnClickListener {
        if (selectedTab != CustomerBottomNavTab.LOYALTY) {
            onCustomerLoyaltyClick()
        }
    }

    bottomNav.navProfile.setOnClickListener {
        if (selectedTab != CustomerBottomNavTab.PROFILE) {
            onCustomerProfileClick()
        }
    }
}

fun LayoutCustomerBottomNavBinding.renderCustomerBottomNav(
    selectedTab: CustomerBottomNavTab,
) {
    bindCustomerBottomNavItem(
        view = navMenu,
        selected = selectedTab == CustomerBottomNavTab.MENU,
    )

    bindCustomerBottomNavItem(
        view = navOrders,
        selected = selectedTab == CustomerBottomNavTab.ORDERS,
    )

    bindCustomerBottomNavItem(
        view = navLoyalty,
        selected = selectedTab == CustomerBottomNavTab.LOYALTY,
    )

    bindCustomerBottomNavItem(
        view = navProfile,
        selected = selectedTab == CustomerBottomNavTab.PROFILE,
    )
}

private fun LayoutCustomerBottomNavBinding.bindCustomerBottomNavItem(
    view: TextView,
    selected: Boolean,
) {
    val context = root.context

    if (selected) {
        view.setBackgroundResource(R.drawable.bg_bottom_nav_item_selected)
    } else {
        view.setBackgroundColor(Color.TRANSPARENT)
    }

    view.setTextColor(
        ContextCompat.getColor(
            context,
            if (selected) {
                R.color.pt_text_dark
            } else {
                R.color.pt_text_primary
            },
        ),
    )
}