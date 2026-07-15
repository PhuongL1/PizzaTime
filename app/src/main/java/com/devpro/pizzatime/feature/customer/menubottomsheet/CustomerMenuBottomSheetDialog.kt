package com.devpro.pizzatime.feature.customer.menubottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.notification.NotificationInboxStore
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.databinding.BottomSheetCustomerMenuBinding
import com.devpro.pizzatime.feature.staff.navigation.openCustomerFavorites
import com.devpro.pizzatime.feature.staff.navigation.openCustomerNotifications
import com.devpro.pizzatime.feature.staff.navigation.openCustomerPromoCodes
import com.devpro.pizzatime.feature.staff.navigation.openPizzaMenuScreen
import com.devpro.pizzatime.feature.staff.navigation.openSupportFaq
import com.devpro.pizzatime.feature.staff.navigation.signOutAndOpenLogin
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.Closeable

class CustomerMenuBottomSheetDialog : BottomSheetDialogFragment(R.layout.bottom_sheet_customer_menu) {

    private var _binding: BottomSheetCustomerMenuBinding? = null
    private val binding: BottomSheetCustomerMenuBinding
        get() = checkNotNull(_binding) {
            "BottomSheetCustomerMenuBinding is only valid between onViewCreated and onDestroyView."
        }
    private var inboxObserver: Closeable? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.ThemeOverlayPizzaTimeBottomSheet)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = BottomSheetCustomerMenuBinding.bind(view)
        NotificationInboxStore.init(requireContext().applicationContext)
        setupActions()
        observeUnreadCount()
    }

    private fun setupActions() = with(binding) {
        btnClose.setOnClickListener {
            dismiss()
        }

        rowMenu.setOnClickListener {
            openPizzaMenuScreen()
        }

        rowOrders.setOnClickListener {
            showComingSoon(getString(R.string.customer_menu_title_orders))
        }

        rowFavorites.setOnClickListener {
            openCustomerFavorites()
        }

        rowPromoCodes.setOnClickListener {
            openCustomerPromoCodes()
        }

        rowMemberQr.setOnClickListener {
            showComingSoon(getString(R.string.customer_menu_title_member_qr))
        }

        rowNotifications.setOnClickListener {
            openCustomerNotifications()
        }

        rowSupport.setOnClickListener {
            openSupportFaq()
        }

        rowLogout.setOnClickListener {
            dismiss()
            signOutAndOpenLogin()
        }
    }

    private fun showComingSoon(label: String) {
        AppUiMessageBus.publish(
            textRes = R.string.customer_menu_selected_toast,
            type = UiMessageType.INFO,
            args = listOf(label),
        )
        dismiss()
    }

    override fun onDestroyView() {
        inboxObserver?.close()
        inboxObserver = null
        _binding = null
        super.onDestroyView()
    }

    private fun observeUnreadCount() {
        binding.tvNotificationsBadge.isVisible = false
        inboxObserver?.close()
        inboxObserver = NotificationInboxStore.observeNotifications {
            if (_binding == null) {
                return@observeNotifications
            }
            renderUnreadCount(NotificationInboxStore.unreadCount())
        }
        NotificationInboxStore.loadForCurrentAccount()
    }

    private fun renderUnreadCount(unreadCount: Int) = with(binding.tvNotificationsBadge) {
        isVisible = unreadCount > 0
        text = if (unreadCount > 99) {
            "99+"
        } else {
            unreadCount.toString()
        }
    }

    companion object {
        private const val TAG = "CustomerMenuBottomSheetDialog"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                CustomerMenuBottomSheetDialog().show(fragmentManager, TAG)
            }
        }
    }
}
