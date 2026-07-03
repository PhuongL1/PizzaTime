package com.devpro.pizzatime.feature.kitchen.board

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav
import com.devpro.pizzatime.databinding.FragmentKitchenBoardBinding
import com.devpro.pizzatime.feature.staff.navigation.backToPreviousStaffScreen
import com.devpro.pizzatime.feature.staff.navigation.openKitchenOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.google.firebase.firestore.ListenerRegistration

class KitchenBoardFragment : Fragment() {

    private var _binding: FragmentKitchenBoardBinding? = null
    private val binding get() = requireNotNull(_binding) {
        "Binding is only valid between onCreateView and onDestroyView"
    }

    private var firestoreOrders: List<KitchenOrderUiModel>? = null
    private var ordersListener: ListenerRegistration? = null

    private val adapter = KitchenOrderAdapter(
        onPrimaryActionClick = { order ->
            handleOrderAction(order)
        },
        onItemClick = { order ->
            openKitchenOrderDetail(order.orderId)
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentKitchenBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupOrders()
        setupBottomNav()
        listenFirestoreOrders()
    }

    private fun setupOrders() = with(binding.rvKitchenOrders) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = this@KitchenBoardFragment.adapter
        this@KitchenBoardFragment.adapter.submitList(FakeKitchenBoardData.getOrders())
    }

    private fun listenFirestoreOrders() {
        ordersListener?.remove()
        ordersListener = KitchenOrderFirestoreRepository.listenOrders { result ->
            if (!isAdded) return@listenOrders
            result.onSuccess { orders ->
                firestoreOrders = orders
                adapter.submitList(orders)
            }
        }
    }

    private fun setupBottomNav() {
        binding.staffBottomNav.setupStaffBottomNav(
            currentTab = StaffBottomNavTab.KITCHEN,
            onDashboardClick = {
                backToPreviousStaffScreen()
            },
            onDeliveryClick = {
                openShipperDeliveryDashboard()
            },
            onProfileClick = {
                showComingSoon(R.string.staff_nav_profile)
            },
        )
    }

    private fun handleOrderAction(order: KitchenOrderUiModel) {
        val messageRes = when (order.status) {
            KitchenOrderStatus.WAITING -> R.string.kitchen_message_baking_started
            KitchenOrderStatus.PREPARING -> R.string.kitchen_message_progress_updated
            KitchenOrderStatus.READY -> R.string.kitchen_message_order_handed_over
            KitchenOrderStatus.NEW -> R.string.kitchen_message_order_accepted
        }

        val nextStatus = nextFirestoreStatus(order.status)
        if (firestoreOrders != null && nextStatus != null) {
            updateFirestoreStatus(order.orderId, nextStatus, messageRes)
        } else {
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFirestoreStatus(orderId: String, nextStatus: String, messageRes: Int) {
        KitchenOrderFirestoreRepository.updateOrderStatus(orderId, nextStatus) { result ->
            if (!isAdded) return@updateOrderStatus
            result
                .onSuccess {
                    Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(requireContext(), "Failed to update order.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun nextFirestoreStatus(status: KitchenOrderStatus): String? {
        return when (status) {
            KitchenOrderStatus.WAITING -> "PREPARING"
            KitchenOrderStatus.PREPARING -> "BAKING"
            KitchenOrderStatus.READY -> "READY"
            KitchenOrderStatus.NEW -> null
        }
    }

    private fun showComingSoon(titleRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(R.string.coming_soon_format, getString(titleRes)),
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
