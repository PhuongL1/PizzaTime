package com.devpro.pizzatime.feature.shipper.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentShipperDeliveryDashboardBinding
import com.devpro.pizzatime.feature.shipper.ShipperOrderFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDetail
import com.devpro.pizzatime.feature.staff.navigation.openStaffDashboard
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav

class ShipperDeliveryDashboardFragment : Fragment(R.layout.fragment_shipper_delivery_dashboard) {

    private var _binding: FragmentShipperDeliveryDashboardBinding? = null
    private val binding: FragmentShipperDeliveryDashboardBinding
        get() = checkNotNull(_binding) {
            "FragmentShipperDeliveryDashboardBinding is only valid between onViewCreated and onDestroyView."
        }

    private val deliveryAdapter = ShipperDeliveryAdapter(
        onStartDeliveryClick = { order ->
            startDelivery(order)
        },
        onItemClick = { order ->
            openShipperDeliveryDetail(order.orderId)
        },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentShipperDeliveryDashboardBinding.bind(view)

        bindActiveDelivery(FakeShipperDeliveryData.getActiveDelivery())
        setupAssignedDeliveries()
        setupBottomNav()
        loadFirestoreOrders()
    }

    private fun bindActiveDelivery(activeDelivery: ShipperDeliveryUiModel) = with(binding) {
        tvActiveOrderId.text = activeDelivery.orderId
        tvActiveEta.text = activeDelivery.etaLabel
        tvActiveCustomerName.text = activeDelivery.customerName
        tvActiveAddress.text = activeDelivery.address
        tvActivePaymentLabel.text = activeDelivery.paymentLabel
        tvActivePaymentAmount.text = activeDelivery.paymentAmount

        btnNavigate.setOnClickListener {
            openShipperDeliveryDetail(activeDelivery.orderId)
        }

        btnCallCustomer.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.shipper_message_call_customer,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupAssignedDeliveries() = with(binding.rvAssignedDeliveries) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = deliveryAdapter
        isNestedScrollingEnabled = false
        deliveryAdapter.submitList(FakeShipperDeliveryData.getAssignedDeliveries())
    }

    private fun loadFirestoreOrders() {
        ShipperOrderFirestoreRepository.loadOrders { result ->
            if (!isAdded) return@loadOrders
            result.onSuccess { orders ->
                val activeDelivery = orders.firstOrNull { it.status == ShipperDeliveryStatus.ACTIVE }
                if (activeDelivery != null) {
                    bindActiveDelivery(activeDelivery)
                }
                deliveryAdapter.submitList(orders.filter { it.status == ShipperDeliveryStatus.ASSIGNED })
            }
        }
    }

    private fun setupBottomNav() {
        binding.staffBottomNav.setupStaffBottomNav(
            currentTab = StaffBottomNavTab.DELIVERY,
            onDashboardClick = {
                openStaffDashboard()
            },
            onKitchenClick = {
                openKitchenBoard()
            },
            onProfileClick = {
                showComingSoon(R.string.staff_nav_profile)
            },
        )
    }

    private fun startDelivery(order: ShipperDeliveryUiModel) {
        openShipperDeliveryDetail(order.orderId)
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