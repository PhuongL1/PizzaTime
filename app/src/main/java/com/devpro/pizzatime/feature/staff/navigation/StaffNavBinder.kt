package com.devpro.pizzatime.feature.staff.navigation

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.customer.account.CustomerProfileFirestoreRepository
import com.devpro.pizzatime.feature.customer.menubottomsheet.CustomerMenuBottomSheetDialog
import com.devpro.pizzatime.shared.drawer.StaffDrawerItem
import com.google.firebase.auth.FirebaseAuth

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
        onMenuClick?.invoke() ?: CustomerMenuBottomSheetDialog.show(parentFragmentManager)
    }

    root.findViewById<View>(R.id.avatarFrame)?.let { avatarFrame ->
        if (onAvatarClick != null) {
            avatarFrame.setOnClickListener { onAvatarClick.invoke() }
        } else {
            avatarFrame.setOnClickListener(null)
        }
    }

    bindCurrentProfileAvatar(
        initialsView = root.findViewById(R.id.tvAdminAvatar),
        imageView = root.findViewById(R.id.ivAdminAvatar),
    )
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
                onDashboardClick?.invoke()
            }
        },
    )

    root.findViewById<TextView>(R.id.navKitchen)?.bindStaffNavItem(
        selected = currentTab == StaffBottomNavTab.KITCHEN,
        onClick = {
            if (currentTab != StaffBottomNavTab.KITCHEN) {
                onKitchenClick?.invoke()
            }
        },
    )

    root.findViewById<TextView>(R.id.navDelivery)?.bindStaffNavItem(
        selected = currentTab == StaffBottomNavTab.DELIVERY,
        onClick = {
            if (currentTab != StaffBottomNavTab.DELIVERY) {
                onDeliveryClick?.invoke()
            }
        },
    )

    root.findViewById<TextView>(R.id.navProfile)?.bindStaffNavItem(
        selected = currentTab == StaffBottomNavTab.PROFILE,
        onClick = {
            if (currentTab != StaffBottomNavTab.PROFILE) {
                onProfileClick?.invoke()
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
        setTextColor(ContextCompat.getColor(context, R.color.staff_nav_selected))
    } else {
        background = null
        setTextColor(ContextCompat.getColor(context, R.color.staff_nav_unselected))
    }
    setOnClickListener { onClick() }
}

fun Fragment.bindCurrentProfileAvatar(
    initialsView: TextView?,
    imageView: ImageView?,
) {
    if (initialsView == null || imageView == null) {
        return
    }

    val fallbackInitials = initialsView.text?.toString().orEmpty()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid.isNullOrBlank()) {
        renderProfileAvatar(
            initialsView = initialsView,
            imageView = imageView,
            fullName = "",
            avatarUrl = "",
            fallbackInitials = fallbackInitials,
        )
        return
    }

    CustomerProfileFirestoreRepository.loadProfile(uid) { result ->
        if (!isAdded) return@loadProfile
        result
            .onSuccess { profile ->
                renderProfileAvatar(
                    initialsView = initialsView,
                    imageView = imageView,
                    fullName = profile.fullName,
                    avatarUrl = profile.avatarUrl,
                    fallbackInitials = fallbackInitials,
                )
            }
            .onFailure {
                renderProfileAvatar(
                    initialsView = initialsView,
                    imageView = imageView,
                    fullName = "",
                    avatarUrl = "",
                    fallbackInitials = fallbackInitials,
                )
            }
    }
}

private fun Fragment.renderProfileAvatar(
    initialsView: TextView,
    imageView: ImageView,
    fullName: String,
    avatarUrl: String,
    fallbackInitials: String,
) {
    initialsView.text = buildAvatarInitials(fullName, fallbackInitials)
    val normalizedAvatarUrl = avatarUrl.trim()
    if (normalizedAvatarUrl.isBlank()) {
        Glide.with(imageView).clear(imageView)
        imageView.setImageDrawable(null)
        imageView.isVisible = false
        initialsView.isVisible = true
        return
    }

    imageView.isVisible = true
    initialsView.isVisible = false
    Glide.with(imageView)
        .load(normalizedAvatarUrl)
        .placeholder(R.drawable.bg_avatar)
        .error(R.drawable.bg_avatar)
        .fallback(R.drawable.bg_avatar)
        .centerCrop()
        .into(imageView)
}

private fun buildAvatarInitials(
    name: String,
    fallback: String,
): String {
    val parts = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return fallback.ifBlank { "PT" }
    return parts
        .take(2)
        .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
        .joinToString(separator = "")
        .ifBlank { fallback.ifBlank { "PT" } }
}
