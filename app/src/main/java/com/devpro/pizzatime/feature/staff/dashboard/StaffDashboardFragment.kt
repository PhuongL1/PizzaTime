package com.devpro.pizzatime.feature.staff.dashboard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentStaffDashboardBinding
import com.devpro.pizzatime.feature.staff.StaffOrderFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.bindStaffBottomNav
import com.devpro.pizzatime.feature.staff.navigation.bindStaffTopBar
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.openStaffOrderDetail
import com.google.firebase.firestore.ListenerRegistration

class StaffDashboardFragment : Fragment(R.layout.fragment_staff_dashboard) {

    private var _binding: FragmentStaffDashboardBinding? = null
    private val binding: FragmentStaffDashboardBinding
        get() = checkNotNull(_binding) {
            "FragmentStaffDashboardBinding is only valid between onViewCreated and onDestroyView."
        }

    private lateinit var staffOrderAdapter: StaffOrderAdapter
    private var selectedStatus = StaffOrderStatus.PENDING
    private var firestoreOrders: List<StaffOrderUiModel>? = null
    private var ordersListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentStaffDashboardBinding.bind(view)

        setupRecyclerView()
        setupStatusChips()
        setupTopBar()
        setupBottomNav()
        renderOrders()
        listenFirestoreOrders()
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
        StaffOrderFirestoreRepository.updateOrderStatus(
            orderId = order.orderId,
            newStatus = "CONFIRMED",
        ) { result ->
            if (!isAdded) return@updateOrderStatus
            result
                .onSuccess {
                    firestoreOrders = firestoreOrders?.map { current ->
                        if (current.orderId == order.orderId) {
                            current.copy(status = StaffOrderStatus.CONFIRMED)
                        } else {
                            current
                        }
                    }
                    showConfirmedToast(order.displayOrderCode)
                    renderOrders()
                }
                .onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Failed to confirm order.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    private fun showConfirmedToast(orderId: String) {
        Toast.makeText(
            requireContext(),
            getString(R.string.staff_order_confirmed_message, orderId),
            Toast.LENGTH_SHORT,
        ).show()
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
        bindStaffBottomNav(
            root = binding.staffBottomNav.root,
            currentTab = StaffBottomNavTab.DASHBOARD,
            onKitchenClick = {
                openKitchenBoard()
            },
            onDeliveryClick = {
                openShipperDeliveryDashboard()
            },
            onProfileClick = {
                openCustomerAccount()
            },
        )
    }

    private fun setupTopBar() {
        bindStaffTopBar(
            root = binding.staffTopBar.root,
            title = getString(R.string.staff_dashboard_title),
        )
    }

    private fun renderOrders() {
        val orders = firestoreOrders
            ?.filter { it.status == selectedStatus }
            .orEmpty()

        staffOrderAdapter.submitList(orders)
        binding.rvStaffOrders.isVisible = orders.isNotEmpty()
        binding.tvEmptyOrders.isVisible = orders.isEmpty()

        updateChipState()
    }

    private fun listenFirestoreOrders() {
        ordersListener?.remove()
        ordersListener = StaffOrderFirestoreRepository.listenOrders { result ->
            if (!isAdded) return@listenOrders
            result.onSuccess { orders ->
                firestoreOrders = orders
                renderOrders()
            }
        }
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
            chip.context.getColor(
                if (isSelected) R.color.staff_nav_selected else R.color.staff_nav_unselected,
            ),
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
        ordersListener?.remove()
        ordersListener = null
        _binding = null
        super.onDestroyView()
    }

}
