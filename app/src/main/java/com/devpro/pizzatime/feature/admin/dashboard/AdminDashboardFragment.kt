package com.devpro.pizzatime.feature.admin.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentAdminDashboardBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav
import com.devpro.pizzatime.feature.admin.navigation.bindAdminTopBar
import com.devpro.pizzatime.feature.staff.navigation.openAddEditProduct
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openManageOrders
import com.devpro.pizzatime.feature.staff.navigation.openManagePromoCodes
import com.devpro.pizzatime.feature.staff.navigation.openManageStaff
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.openReports
import com.devpro.pizzatime.feature.staff.navigation.openStoreSettings

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding: FragmentAdminDashboardBinding
        get() = checkNotNull(_binding) {
            "FragmentAdminDashboardBinding is only valid between onViewCreated and onDestroyView."
        }

    private val recentOrderAdapter = AdminRecentOrderAdapter(
        onOrderClick = { order ->
            showUiMessage(
                textRes = R.string.admin_recent_order_clicked,
                type = UiMessageType.INFO,
                args = listOf(order.displayOrderCode),
            )
        },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAdminDashboardBinding.bind(view)

        bindDashboard(emptyDashboard())
        setupTopBar()
        setupQuickActions()
        setupRecentOrders()
        setupBottomNav()
        loadFirestoreData()
    }

    private fun loadFirestoreData() {
        AdminDashboardFirestoreRepository.loadDashboard { result ->
            if (_binding == null || !isAdded) return@loadDashboard
            result
                .onSuccess(::bindDashboard)
                .onFailure { error ->
                    Log.e(TAG, "Failed to load admin dashboard", error)
                    showUiMessage(R.string.admin_dashboard_load_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun bindDashboard(data: AdminDashboardUiModel) = with(binding) {
        tvTotalRevenue.text = data.totalRevenue
        tvRevenueGrowth.text = data.revenueGrowth
        tvTodayTotal.text = data.todayTotal
        tvPendingCount.text = data.pendingCount
        tvCompletedCount.text = data.completedCount
        tvSatisfactionLabel.text = data.satisfactionLabel

        val recentOrders = data.recentOrders.take(RECENT_ORDER_LIMIT)
        recentOrderAdapter.submitList(recentOrders)
        rvRecentOrders.isVisible = recentOrders.isNotEmpty()
        tvRecentOrdersEmpty.isVisible = recentOrders.isEmpty()
    }

    private fun setupQuickActions() = with(binding) {
        btnAddProduct.setOnClickListener {
            openAddEditProduct()
        }

        btnManageStaff.setOnClickListener {
            openManageStaff()
        }

        btnViewReports.setOnClickListener {
            openReports()
        }

        btnInventory.setOnClickListener {
            openManageMenu()
        }

        btnPromotions.setOnClickListener {
            openManagePromoCodes()
        }

        btnStoreSettings.setOnClickListener {
            openStoreSettings()
        }

        tvSeeAllOrders.setOnClickListener {
            openManageOrders()
        }
    }

    private fun setupRecentOrders() = with(binding.rvRecentOrders) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = recentOrderAdapter
        isNestedScrollingEnabled = false
    }

    private fun setupBottomNav() {
        bindAdminBottomNav(
            root = binding.staffBottomNav.root,
            selectedDestination = AdminBottomNavDestination.DASHBOARD,
            onDashboardClick = { openAdminDashboard() },
            onManageMenuClick = { openManageMenu() },
            onManagePromoCodesClick = { openShipperDeliveryDashboard() },
            onManageStaffClick = { openCustomerAccount() },
        )
    }

    private fun setupTopBar() {
        bindAdminTopBar(root = binding.staffTopBar.root)
    }

    private fun emptyDashboard(): AdminDashboardUiModel {
        return AdminDashboardUiModel(
            totalRevenue = "$0.00",
            revenueGrowth = "",
            todayTotal = "0",
            pendingCount = "0",
            completedCount = "0 Delivered",
            satisfactionLabel = "",
            recentOrders = emptyList(),
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "AdminDashboard"
        private const val RECENT_ORDER_LIMIT = 3
    }
}
