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
import com.devpro.pizzatime.feature.staff.navigation.bindStaffBottomNav
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDetail
import com.devpro.pizzatime.feature.staff.navigation.openStaffDashboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

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
    private var ordersListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentShipperDeliveryDashboardBinding.bind(view)

        bindActiveDelivery(FakeShipperDeliveryData.getActiveDelivery())
        setupAssignedDeliveries()
        setupBottomNav()
        listenFirestoreOrders()
    }

    private fun bindActiveDelivery(activeDelivery: ShipperDeliveryUiModel) = with(binding) {
        tvActiveOrderId.text = activeDelivery.displayOrderCode
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

    private fun listenFirestoreOrders() {
        ordersListener?.remove()
        ordersListener = ShipperOrderFirestoreRepository.listenOrders { result ->
            if (!isAdded) return@listenOrders
            result.onSuccess { orders ->
                val shipperId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                val visibleOrders = orders.filter { order ->
                    order.shipperId.isBlank() || order.shipperId == shipperId
                }
                val activeDelivery = visibleOrders.firstOrNull { it.status == ShipperDeliveryStatus.ACTIVE }
                if (activeDelivery != null) {
                    bindActiveDelivery(activeDelivery)
                }
                deliveryAdapter.submitList(visibleOrders)
            }
        }
    }

    private fun setupBottomNav() {
        bindStaffBottomNav(
            root = binding.staffBottomNav.root,
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
        ordersListener?.remove()
        ordersListener = null
        _binding = null
        super.onDestroyView()
    }
}
