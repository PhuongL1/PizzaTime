package com.devpro.pizzatime.feature.customer.notifications

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.core.notification.AppNotification
import com.devpro.pizzatime.core.notification.NotificationDeepLink
import com.devpro.pizzatime.core.notification.NotificationInboxStore
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentCustomerNotificationsBinding
import com.devpro.pizzatime.feature.customer.account.CustomerProfileFirestoreRepository
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openKitchenOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openManageOrders
import com.devpro.pizzatime.feature.staff.navigation.openOrderTracking
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDetail
import com.devpro.pizzatime.feature.staff.navigation.openStaffOrderDetail
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class CustomerNotificationsFragment : Fragment(R.layout.fragment_customer_notifications) {

    private var _binding: FragmentCustomerNotificationsBinding? = null
    private val binding: FragmentCustomerNotificationsBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerNotificationsBinding is only valid between onViewCreated and onDestroyView."
        }

    private val adapter = CustomerNotificationAdapter(
        onNotificationClick = ::openNotification,
    )
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCustomerNotificationsBinding.bind(view)
        NotificationInboxStore.init(requireContext().applicationContext)
        setupRecyclerView()
        setupActions()
        setupBottomNav()
        loadCustomerAvatar()
        observeInbox()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadCustomerAvatar()
            NotificationInboxStore.refreshForCurrentAccount()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() = with(binding.rvNotifications) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = this@CustomerNotificationsFragment.adapter
        clipToPadding = false
    }

    private fun setupActions() = with(binding) {
        btnMarkAllRead.setOnClickListener {
            NotificationInboxStore.markAllRead()
        }
    }

    private fun setupBottomNav() = with(binding) {
        bindCustomerBottomNav(
            root = bottomNav.root,
            selectedTab = CustomerBottomNavTab.ORDERS,
        )
    }

    private fun observeInbox() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificationInboxStore.notifications.collect { notifications ->
                    if (_binding != null) {
                        renderNotifications(notifications)
                    }
                }
            }
        }
    }

    private fun renderNotifications(notifications: List<AppNotification>) {
        val signedIn = FirebaseAuth.getInstance().currentUser != null
        if (!signedIn) {
            binding.rvNotifications.isVisible = false
            binding.emptyState.isVisible = true
            binding.tvEmptyTitle.setText(R.string.customer_notifications_empty_title)
            binding.tvEmptyMessage.setText(R.string.customer_notifications_login_required_message)
            binding.btnMarkAllRead.isVisible = false
            adapter.submitList(emptyList())
            return
        }

        val items = notifications.sortedByDescending { notification -> notification.createdAtMillis }
            .map { notification -> notification.toUiModel() }
        val hasNotifications = items.isNotEmpty()
        binding.rvNotifications.isVisible = hasNotifications
        binding.emptyState.isVisible = !hasNotifications
        binding.btnMarkAllRead.isVisible = hasNotifications
        binding.btnMarkAllRead.isEnabled = notifications.any { notification -> !notification.isRead }
        binding.tvEmptyTitle.setText(R.string.customer_notifications_empty_title)
        binding.tvEmptyMessage.setText(R.string.customer_notifications_empty_message)
        adapter.submitList(items)
    }

    private fun loadCustomerAvatar() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CustomerProfileFirestoreRepository.loadProfile(uid) { result ->
            if (_binding == null) return@loadProfile
            result.onSuccess { profile ->
                binding.ivAvatar.loadProductImage(
                    profile.avatarUrl,
                    R.drawable.ic_customer_account_avatar_placeholder,
                )
            }
        }
    }

    private fun openNotification(notification: CustomerNotificationUiModel) {
        NotificationInboxStore.markRead(notification.id)
        when (notification.deepLinkType) {
            NotificationDeepLink.CUSTOMER_ORDER_TRACKING -> {
                val orderId = notification.orderId.orEmpty()
                if (orderId.isBlank()) {
                    showUiMessage(R.string.notification_order_unavailable, UiMessageType.ERROR)
                } else {
                    openOrderTracking(orderId)
                }
            }

            NotificationDeepLink.CUSTOMER_ORDER_DETAIL -> {
                val orderId = notification.orderId.orEmpty()
                if (orderId.isBlank()) {
                    showUiMessage(R.string.notification_order_unavailable, UiMessageType.ERROR)
                } else {
                    openCustomerOrderDetail(orderId)
                }
            }

            NotificationDeepLink.STAFF_ORDER_DETAIL -> {
                val orderId = notification.orderId.orEmpty()
                if (orderId.isBlank()) {
                    showUiMessage(R.string.notification_order_unavailable, UiMessageType.ERROR)
                } else {
                    openStaffOrderDetail(orderId)
                }
            }

            NotificationDeepLink.KITCHEN_ORDER_DETAIL -> {
                val orderId = notification.orderId.orEmpty()
                if (orderId.isBlank()) {
                    showUiMessage(R.string.notification_order_unavailable, UiMessageType.ERROR)
                } else {
                    openKitchenOrderDetail(orderId)
                }
            }

            NotificationDeepLink.SHIPPER_ORDER_DETAIL -> {
                val orderId = notification.orderId.orEmpty()
                if (orderId.isBlank()) {
                    showUiMessage(R.string.notification_order_unavailable, UiMessageType.ERROR)
                } else {
                    openShipperDeliveryDetail(orderId)
                }
            }

            NotificationDeepLink.ADMIN_ORDER_DETAIL -> {
                val orderId = notification.orderId.orEmpty()
                if (orderId.isBlank()) {
                    showUiMessage(R.string.notification_order_unavailable, UiMessageType.ERROR)
                } else {
                    openStaffOrderDetail(orderId)
                }
            }

            NotificationDeepLink.ADMIN_REVIEW_DETAIL -> {
                val orderId = notification.orderId.orEmpty()
                if (orderId.isBlank()) {
                    openManageOrders()
                } else {
                    openStaffOrderDetail(orderId)
                }
            }

            NotificationDeepLink.NONE -> Unit
        }
    }

    private fun AppNotification.toUiModel(): CustomerNotificationUiModel {
        val iconStyle = when (deepLinkType) {
            NotificationDeepLink.CUSTOMER_ORDER_TRACKING,
            NotificationDeepLink.CUSTOMER_ORDER_DETAIL,
            -> Triple(R.drawable.ic_bag, R.drawable.bg_customer_notifications_icon_warm, R.color.pt_copper)

            else -> Triple(R.drawable.customer_menu_icon_notifications, R.drawable.bg_customer_notifications_icon_neutral, R.color.pt_text_primary)
        }
        return CustomerNotificationUiModel(
            id = id,
            title = title,
            body = body,
            timestampLabel = DateUtils.getRelativeTimeSpanString(
                createdAtMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            ).toString(),
            isUnread = !isRead,
            orderId = orderId,
            reviewId = reviewId,
            deepLinkType = deepLinkType,
            iconRes = iconStyle.first,
            iconBackgroundRes = iconStyle.second,
            iconTintRes = iconStyle.third,
        )
    }
}
