package com.devpro.pizzatime.feature.customer.common.navigation

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R

fun Fragment.bindPizzaFlowTopBar(
    root: View,
    cartItemCount: Int = 0,
    onBackClick: () -> Unit,
    onCartClick: () -> Unit,
) {
    root.findViewById<View>(R.id.btnBack)?.setOnClickListener {
        onBackClick()
    }

    root.findViewById<View>(R.id.btnCart)?.setOnClickListener {
        onCartClick()
    }

    updatePizzaFlowCartBadge(root, cartItemCount)
}

fun updatePizzaFlowCartBadge(
    root: View,
    cartItemCount: Int,
) {
    root.findViewById<TextView>(R.id.tvCartBadge)?.let { badge ->
        badge.isVisible = cartItemCount > 0
        badge.text = when {
            cartItemCount <= 0 -> ""
            cartItemCount > MAX_BADGE_COUNT -> MAX_BADGE_TEXT
            else -> cartItemCount.toString()
        }
    }
}

private const val MAX_BADGE_COUNT = 99
private const val MAX_BADGE_TEXT = "99+"
