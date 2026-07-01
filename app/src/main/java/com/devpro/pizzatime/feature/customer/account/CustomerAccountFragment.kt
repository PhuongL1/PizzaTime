package com.devpro.pizzatime.feature.customer.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerAccountBinding
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.bottomnav.setupCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.topbar.setupCustomerTopBar
import java.text.NumberFormat
import java.util.Locale

class CustomerAccountFragment : Fragment() {

    private var _binding: FragmentCustomerAccountBinding? = null
    private val binding: FragmentCustomerAccountBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerAccountBinding is only valid between onCreateView and onDestroyView."
        }

    private val accountData: CustomerAccountUiModel = FakeCustomerAccountData.getCustomerAccount()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindAccount()
        setupTopBar()
        setupBottomNav()
        setupActions()
    }

    private fun bindAccount() = with(binding) {
        ivAvatar.setImageResource(accountData.avatarRes)
        tvCustomerName.text = accountData.fullName
        tvTierName.text = accountData.tierName
        tvDoughPoints.text = getString(
            R.string.customer_account_dough_points,
            formatNumber(accountData.doughPoints),
        )
        tvEmailValue.text = accountData.email
        tvPhoneValue.text = accountData.phone
    }

    private fun setupTopBar() = with(binding) {
        setupCustomerTopBar(
            topBar = customerTopBar,
            cartItemCount = 0,
            onCartClick = {
                showToast(getString(R.string.customer_account_cart_toast))
            },
        )
    }

    private fun setupBottomNav() = with(binding) {
        setupCustomerBottomNav(
            bottomNav = customerBottomNav,
            selectedTab = CustomerBottomNavTab.PROFILE,
            onCustomerMenuClick = {
                showToast(getString(R.string.customer_account_menu_toast))
            },
            onCustomerOrdersClick = {
                showToast(getString(R.string.customer_account_orders_toast))
            },
            onCustomerLoyaltyClick = {
                showToast(getString(R.string.customer_account_loyalty_toast))
            },
        )
    }

    private fun setupActions() = with(binding) {
        editAvatarButton.setOnClickListener {
            showToast(getString(R.string.customer_account_edit_profile_toast))
        }

        rowOrderHistory.setOnClickListener {
            showToast(getString(R.string.customer_account_order_history_toast))
        }

        rowPaymentMethods.setOnClickListener {
            showToast(getString(R.string.customer_account_payment_methods_toast))
        }

        rowDeliveryAddresses.setOnClickListener {
            showToast(getString(R.string.customer_account_delivery_addresses_toast))
        }

        rowFavorites.setOnClickListener {
            showToast(getString(R.string.customer_account_favorites_toast))
        }

        rowSettings.setOnClickListener {
            showToast(getString(R.string.customer_account_settings_toast))
        }

        logoutCard.setOnClickListener {
            showToast(getString(R.string.customer_account_logout_toast))
        }
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}