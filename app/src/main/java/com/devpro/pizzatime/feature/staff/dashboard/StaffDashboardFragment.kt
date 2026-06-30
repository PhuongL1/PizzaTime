package com.devpro.pizzatime.feature.staff.dashboard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentStaffDashboardBinding
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.openStaffOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav

class StaffDashboardFragment : Fragment(R.layout.fragment_staff_dashboard) {

    private var _binding: FragmentStaffDashboardBinding? = null
    private val binding: FragmentStaffDashboardBinding
        get() = checkNotNull(_binding) {
            "FragmentStaffDashboardBinding is only valid between onViewCreated and onDestroyView."
        }

    private lateinit var staffOrderAdapter: StaffOrderAdapter
    private var selectedStatus = StaffOrderStatus.PENDING

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentStaffDashboardBinding.bind(view)

        setupRecyclerView()
        setupStatusChips()
        setupBottomNav()
        renderOrders()
    }

    private fun setupRecyclerView() {
        staffOrderAdapter = StaffOrderAdapter(
            onConfirmClick = { order ->
                confirmOrder(order)
            },
            onDetailClick = { order ->
                openStaffOrderDetail(order.orderId)
            },
        )

        binding.rvStaffOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = staffOrderAdapter
        }
    }

    private fun confirmOrder(order: StaffOrderUiModel) {
        FakeStaffDashboardData.confirmOrder(order.orderId)

        Toast.makeText(
            requireContext(),
            getString(R.string.staff_order_confirmed_message, order.orderId),
            Toast.LENGTH_SHORT,
        ).show()

        renderOrders()
    }

    private fun setupStatusChips() = with(binding) {
        chipNewOrders.setOnClickListener {
            selectStatus(StaffOrderStatus.PENDING)
        }

        chipConfirmed.setOnClickListener {
            selectStatus(StaffOrderStatus.CONFIRMED)
        }

        chipPreparing.setOnClickListener {
            selectStatus(StaffOrderStatus.PREPARING)
        }

        chipReady.setOnClickListener {
            selectStatus(StaffOrderStatus.READY)
        }
    }

    private fun selectStatus(status: StaffOrderStatus) {
        selectedStatus = status
        renderOrders()
    }

    private fun setupBottomNav() {
        binding.staffBottomNav.setupStaffBottomNav(
            currentTab = StaffBottomNavTab.DASHBOARD,
            onKitchenClick = {
                openKitchenBoard()
            },
            onDeliveryClick = {
                openShipperDeliveryDashboard()
            },
            onProfileClick = {
                showComingSoon(R.string.staff_nav_profile)
            },
        )
    }

    private fun renderOrders() {
        val orders = FakeStaffDashboardData.getOrdersByStatus(selectedStatus)

        staffOrderAdapter.submitList(orders)
        binding.rvStaffOrders.isVisible = orders.isNotEmpty()
        binding.tvEmptyOrders.isVisible = orders.isEmpty()

        updateChipState()
    }

    private fun updateChipState() = with(binding) {
        setChipSelected(chipNewOrders, selectedStatus == StaffOrderStatus.PENDING)
        setChipSelected(chipConfirmed, selectedStatus == StaffOrderStatus.CONFIRMED)
        setChipSelected(chipPreparing, selectedStatus == StaffOrderStatus.PREPARING)
        setChipSelected(chipReady, selectedStatus == StaffOrderStatus.READY)
    }

    private fun setChipSelected(chip: TextView, isSelected: Boolean) {
        chip.setBackgroundResource(
            if (isSelected) R.drawable.bg_chip_selected_gold else R.drawable.bg_chip_unselected_dark,
        )

        chip.setTextColor(
            if (isSelected) COLOR_CHIP_SELECTED else COLOR_CHIP_UNSELECTED,
        )
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

    companion object {
        private val COLOR_CHIP_SELECTED = "#3A210D".toColorInt()
        private val COLOR_CHIP_UNSELECTED = "#E6D4C8".toColorInt()
    }
}