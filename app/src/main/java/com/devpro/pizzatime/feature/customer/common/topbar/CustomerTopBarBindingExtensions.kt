package com.devpro.pizzatime.feature.customer.common.topbar

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.databinding.LayoutCustomerTopBarBinding
import com.devpro.pizzatime.feature.customer.menubottomsheet.CustomerMenuBottomSheetDialog

fun Fragment.setupCustomerTopBar(
    topBar: LayoutCustomerTopBarBinding,
    cartItemCount: Int = 0,
    onCartClick: () -> Unit,
) {
    topBar.btnMenu.setOnClickListener {
        CustomerMenuBottomSheetDialog.show(parentFragmentManager)
    }

    topBar.cartButtonContainer.setOnClickListener {
        onCartClick()
    }

    topBar.renderCartBadge(cartItemCount)
}

fun LayoutCustomerTopBarBinding.renderCartBadge(cartItemCount: Int) {
    tvCartBadge.isVisible = cartItemCount > 0
    tvCartBadge.text = when {
        cartItemCount <= 0 -> ""
        cartItemCount > MAX_BADGE_COUNT -> MAX_BADGE_TEXT
        else -> cartItemCount.toString()
    }
}

private const val MAX_BADGE_COUNT = 99
private const val MAX_BADGE_TEXT = "99+"