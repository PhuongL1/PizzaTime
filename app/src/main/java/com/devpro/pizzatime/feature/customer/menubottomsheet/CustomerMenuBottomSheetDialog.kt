package com.devpro.pizzatime.feature.customer.menubottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.BottomSheetCustomerMenuBinding
import com.devpro.pizzatime.feature.staff.navigation.signOutAndOpenLogin
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CustomerMenuBottomSheetDialog : BottomSheetDialogFragment(R.layout.bottom_sheet_customer_menu) {

    private var _binding: BottomSheetCustomerMenuBinding? = null
    private val binding: BottomSheetCustomerMenuBinding
        get() = checkNotNull(_binding) {
            "BottomSheetCustomerMenuBinding is only valid between onViewCreated and onDestroyView."
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.ThemeOverlayPizzaTimeBottomSheet)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = BottomSheetCustomerMenuBinding.bind(view)
        setupActions()
    }

    private fun setupActions() = with(binding) {
        btnClose.setOnClickListener {
            dismiss()
        }

        rowMenu.setOnClickListener {
            showComingSoon(getString(R.string.customer_menu_title_menu))
        }

        rowOrders.setOnClickListener {
            showComingSoon(getString(R.string.customer_menu_title_orders))
        }

        rowFavorites.setOnClickListener {
            showComingSoon(getString(R.string.customer_menu_title_favorites))
        }

        rowPromoCodes.setOnClickListener {
            showComingSoon(getString(R.string.customer_menu_title_promo_codes))
        }

        rowMemberQr.setOnClickListener {
            showComingSoon(getString(R.string.customer_menu_title_member_qr))
        }

        rowNotifications.setOnClickListener {
            showComingSoon(getString(R.string.customer_menu_title_notifications))
        }

        rowSupport.setOnClickListener {
            showComingSoon(getString(R.string.customer_menu_title_support))
        }

        rowLogout.setOnClickListener {
            dismiss()
            signOutAndOpenLogin()
        }
    }

    private fun showComingSoon(label: String) {
        Toast.makeText(
            requireContext(),
            getString(R.string.customer_menu_selected_toast, label),
            Toast.LENGTH_SHORT,
        ).show()
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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
