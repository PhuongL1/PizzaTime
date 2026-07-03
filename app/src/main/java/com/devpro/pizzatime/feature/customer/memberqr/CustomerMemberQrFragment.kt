package com.devpro.pizzatime.feature.customer.memberqr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerMemberQrBinding
import com.devpro.pizzatime.feature.customer.account.CustomerProfileFirestoreRepository
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.bottomnav.setupCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.topbar.setupCustomerTopBar
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.Locale

class CustomerMemberQrFragment : Fragment() {

    private var _binding: FragmentCustomerMemberQrBinding? = null
    private val binding: FragmentCustomerMemberQrBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerMemberQrBinding is only valid between onCreateView and onDestroyView."
        }

    private var memberQrData: CustomerMemberQrUiModel = FakeCustomerMemberQrData.getMemberQr()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerMemberQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindMemberQr()
        setupTopBar()
        setupBottomNav()
        setupActions()
        loadMemberProfile()
    }

    private fun bindMemberQr() = with(binding) {
        tvTierLabel.text = memberQrData.tierLabel
        tvMemberTitle.text = memberQrData.memberTitle
        tvPointsLabel.text = memberQrData.pointsLabel
        tvPointsValue.text = getString(
            R.string.customer_member_qr_points_value,
            formatNumber(memberQrData.currentPoints),
            formatNumber(memberQrData.targetPoints),
        )
        tvMemberSinceLabel.text = memberQrData.memberSinceLabel
        tvMemberSinceValue.text = memberQrData.memberSinceValue
        tvQrInstruction.text = memberQrData.qrInstruction

        memberProgress.max = memberQrData.targetPoints
        memberProgress.progress = memberQrData.currentPoints

        ivQrImage.setImageResource(memberQrData.qrImageRes)
    }

    private fun setupTopBar() = with(binding) {
        setupCustomerTopBar(
            topBar = customerTopBar,
            cartItemCount = 0,
            onCartClick = {
                showToast(getString(R.string.customer_member_qr_cart_toast))
            },
        )
    }

    private fun setupBottomNav() = with(binding) {
        setupCustomerBottomNav(
            bottomNav = customerBottomNav,
            selectedTab = CustomerBottomNavTab.PROFILE,
            onCustomerMenuClick = {
                showToast(getString(R.string.customer_member_qr_menu_toast))
            },
            onCustomerOrdersClick = {
                showToast(getString(R.string.customer_member_qr_orders_toast))
            },
            onCustomerLoyaltyClick = {
                showToast(getString(R.string.customer_member_qr_loyalty_toast))
            },
        )
    }

    private fun setupActions() = with(binding) {
        historyCard.setOnClickListener {
            showToast(getString(R.string.customer_member_qr_history_toast))
        }

        rewardsCard.setOnClickListener {
            showToast(getString(R.string.customer_member_qr_rewards_toast))
        }
    }

    private fun loadMemberProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            return
        }

        CustomerProfileFirestoreRepository.loadProfile(uid) { result ->
            if (!isAdded) return@loadProfile
            result.onSuccess { profile ->
                memberQrData = memberQrData.copy(
                    memberTitle = profile.fullName,
                    currentPoints = profile.doughPoints,
                    targetPoints = maxOf(memberQrData.targetPoints, profile.doughPoints),
                    memberSinceLabel = "CUSTOMER ID",
                    memberSinceValue = uid.take(12),
                )
                bindMemberQr()
            }
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

    companion object {
        fun newInstance(): CustomerMemberQrFragment = CustomerMemberQrFragment()
    }
}
