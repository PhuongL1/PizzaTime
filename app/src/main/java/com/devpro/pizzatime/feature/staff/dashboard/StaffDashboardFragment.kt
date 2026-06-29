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
import com.devpro.pizzatime.feature.staff.detail.StaffOrderDetailFragment

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
                FakeStaffDashboardData.confirmOrder(order.orderId)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.staff_order_confirmed_message, order.orderId),
                    Toast.LENGTH_SHORT,
                ).show()
                renderOrders()
            },
            onDetailClick = { order ->
                openOrderDetail(order.orderId)
            },
        )

        binding.rvStaffOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = staffOrderAdapter
        }
    }

    private fun setupStatusChips() = with(binding) {
        chipNewOrders.setOnClickListener {
            selectedStatus = StaffOrderStatus.PENDING
            renderOrders()
        }

        chipConfirmed.setOnClickListener {
            selectedStatus = StaffOrderStatus.CONFIRMED
            renderOrders()
        }

        chipPreparing.setOnClickListener {
            selectedStatus = StaffOrderStatus.PREPARING
            renderOrders()
        }

        chipReady.setOnClickListener {
            selectedStatus = StaffOrderStatus.READY
            renderOrders()
        }
    }

    private fun setupBottomNav() = with(binding.staffBottomNav) {
        navDashboard.setOnClickListener {
            // Already on dashboard.
        }

        navKitchen.setOnClickListener {
            showComingSoon(R.string.staff_nav_kitchen)
        }

        navDelivery.setOnClickListener {
            showComingSoon(R.string.staff_nav_delivery)
        }

        navProfile.setOnClickListener {
            showComingSoon(R.string.staff_nav_profile)
        }
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
        val backgroundRes = if (isSelected) {
            R.drawable.bg_chip_selected_gold
        } else {
            R.drawable.bg_chip_unselected_dark
        }

        val textColor = if (isSelected) {
            "#3A210D"
        } else {
            "#E6D4C8"
        }

        chip.setBackgroundResource(backgroundRes)
        chip.setTextColor(textColor.toColorInt())
    }

    private fun showComingSoon(titleRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(R.string.staff_coming_soon_message, getString(titleRes)),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun openOrderDetail(orderId: String) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, StaffOrderDetailFragment.newInstance(orderId))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}