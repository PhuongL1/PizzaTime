package com.devpro.pizzatime.core.ui.notification

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.notification.NotificationInboxStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

fun TextView.renderUnreadNotificationCount(count: Int) {
    val state = notificationBadgeState(
        count = count,
        overflowText = context.getString(R.string.notification_badge_overflow),
    )
    text = state.text
    isVisible = state.isVisible
}

fun Fragment.bindNotificationBadge(
    badgeView: TextView,
    menuButton: View,
) {
    (badgeView.getTag(R.id.notification_badge_binding_job) as? Job)?.cancel()
    renderNotificationBadge(badgeView, menuButton, count = 0)
    NotificationInboxStore.init(requireContext().applicationContext)

    val bindingJob = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            NotificationInboxStore.refreshForCurrentAccount()
            NotificationInboxStore.unreadCount
                .catch { error ->
                    Log.w(TAG, "Unread count collection failed", error)
                    emit(0)
                }
                .collect { count ->
                    renderNotificationBadge(badgeView, menuButton, count)
                }
        }
    }
    badgeView.setTag(R.id.notification_badge_binding_job, bindingJob)
}

private fun Fragment.renderNotificationBadge(
    badgeView: TextView,
    menuButton: View,
    count: Int,
) {
    val visibleCount = count.takeIf { menuButton.visibility == View.VISIBLE } ?: 0
    badgeView.renderUnreadNotificationCount(visibleCount)
    menuButton.contentDescription = if (count <= 0) {
        getString(R.string.notification_badge_menu)
    } else {
        resources.getQuantityString(
            R.plurals.notification_badge_menu_unread,
            count,
            count,
        )
    }
}

private const val TAG = "NotificationBadge"
