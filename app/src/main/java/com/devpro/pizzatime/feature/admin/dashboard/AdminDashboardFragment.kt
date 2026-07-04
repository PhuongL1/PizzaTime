package com.devpro.pizzatime.feature.admin.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentAdminDashboardBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav
import com.devpro.pizzatime.feature.admin.navigation.bindAdminTopBar
import com.devpro.pizzatime.feature.staff.navigation.openAddEditProduct
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openManagePromoCodes
import com.devpro.pizzatime.feature.staff.navigation.openManageStaff
import com.devpro.pizzatime.feature.staff.navigation.openReports

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding: FragmentAdminDashboardBinding
        get() = checkNotNull(_binding) {
            "FragmentAdminDashboardBinding is only valid between onViewCreated and onDestroyView."
        }

    private val recentOrderAdapter = AdminRecentOrderAdapter(
        onOrderClick = { order ->
            Toast.makeText(
                requireContext(),
                getString(R.string.admin_recent_order_clicked, order.orderId),
                Toast.LENGTH_SHORT,
            ).show()
        },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAdminDashboardBinding.bind(view)

        bindDashboard(FakeAdminDashboardData.getDashboard())
        setupTopBar()
        setupQuickActions()
        setupRecentOrders()
        setupBottomNav()
        loadFirestoreData()
    }

    private fun loadFirestoreData() {
        AdminDashboardFirestoreRepository.loadDashboard { result ->
            if (!isAdded) return@loadDashboard
            result.onSuccess { data -> bindDashboard(data) }
        }
    }

    private fun bindDashboard(data: AdminDashboardUiModel) = with(binding) {
        tvTotalRevenue.text = data.totalRevenue
        tvRevenueGrowth.text = data.revenueGrowth
        tvTodayTotal.text = data.todayTotal
        tvPendingCount.text = data.pendingCount
        tvCompletedCount.text = data.completedCount
        tvSatisfactionLabel.text = data.satisfactionLabel

        recentOrderAdapter.submitList(data.recentOrders)
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

        tvSeeAllOrders.setOnClickListener {
            showComingSoon(R.string.admin_see_all)
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
        )
    }

    private fun setupTopBar() {
        bindAdminTopBar(root = binding.staffTopBar.root)
    }

    private fun showComingSoon(titleRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(R.string.staff_coming_soon_message, getString(titleRes)),
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
