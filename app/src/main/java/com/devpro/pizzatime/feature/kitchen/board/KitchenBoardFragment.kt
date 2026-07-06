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
import com.devpro.pizzatime.feature.staff.navigation.bindStaffBottomNav
import com.devpro.pizzatime.databinding.FragmentKitchenBoardBinding
import com.devpro.pizzatime.feature.staff.navigation.backToPreviousStaffScreen
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openKitchenOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.openStaffDashboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        loadChefFirstName()
        listenFirestoreOrders()
    }

    private fun setupOrders() = with(binding.rvKitchenOrders) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = this@KitchenBoardFragment.adapter
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
        bindStaffBottomNav(
            root = binding.staffBottomNav.root,
            currentTab = StaffBottomNavTab.KITCHEN,
            onDashboardClick = {
                openStaffDashboard()
            },
            onDeliveryClick = {
                openShipperDeliveryDashboard()
            },
            onProfileClick = {
                openCustomerAccount()
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
                .onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Failed to update order.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    private fun nextFirestoreStatus(status: KitchenOrderStatus): String? {
        return when (status) {
            KitchenOrderStatus.WAITING -> "PREPARING"
            KitchenOrderStatus.PREPARING -> "BAKING"
            KitchenOrderStatus.READY -> "READY_FOR_DELIVERY"
            KitchenOrderStatus.NEW -> null
        }
    }

    private fun loadChefFirstName() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                val profileName = document.getString("name").orEmpty()
                binding.tvKitchenTitle.text = profileName.toChefFirstName(
                    fallback = user.email.orEmpty().substringBefore("@"),
                )
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.tvKitchenTitle.text = user.email.orEmpty()
                    .substringBefore("@")
                    .ifBlank { getString(R.string.kitchen_default_chef_name) }
            }
    }

    private fun String.toChefFirstName(fallback: String): String {
        val parts = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isNotEmpty() -> parts.last()
            fallback.isNotBlank() -> fallback
            else -> getString(R.string.kitchen_default_chef_name)
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
