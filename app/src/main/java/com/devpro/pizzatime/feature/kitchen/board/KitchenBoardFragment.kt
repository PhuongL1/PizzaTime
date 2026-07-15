package com.devpro.pizzatime.feature.kitchen.board

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.bindStaffTopBar
import com.devpro.pizzatime.feature.staff.navigation.bindStaffBottomNav
import com.devpro.pizzatime.databinding.FragmentKitchenBoardBinding
import com.devpro.pizzatime.feature.staff.navigation.canManageKitchenScreen
import com.devpro.pizzatime.feature.staff.navigation.directionTo
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openKitchenOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.openStaffDashboard
import com.google.firebase.firestore.ListenerRegistration

class KitchenBoardFragment : Fragment() {

    private var _binding: FragmentKitchenBoardBinding? = null
    private val binding get() = requireNotNull(_binding) {
        "Binding is only valid between onCreateView and onDestroyView"
    }

    private var allOrders: List<KitchenOrderUiModel> = emptyList()
    private var selectedFilter: KitchenFilter = KitchenFilter.WAITING
    private var ordersListener: ListenerRegistration? = null
    private var hasShownOrdersLoadError = false

    private val adapter = KitchenOrderAdapter(
        onPrimaryActionClick = { order ->
            handleOrderAction(order)
        },
        onItemClick = { order ->
            openKitchenOrderDetail(order.orderId)
        },
        canManageActions = { canManageKitchenScreen() },
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
        setupTopBar()
        setupBottomNav()
        setupFilters()
        listenFirestoreOrders()
    }

    private fun setupOrders() = with(binding.rvKitchenOrders) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = this@KitchenBoardFragment.adapter
    }

    private fun listenFirestoreOrders() {
        ordersListener?.remove()
        ordersListener = KitchenOrderFirestoreRepository.listenOrders { result ->
            if (_binding == null || !isAdded) return@listenOrders
            result
                .onSuccess { orders ->
                    hasShownOrdersLoadError = false
                    allOrders = orders.filter { it.status != KitchenOrderStatus.READY }
                    renderSelectedFilter()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to listen for kitchen orders", error)
                    if (!hasShownOrdersLoadError) {
                        hasShownOrdersLoadError = true
                        showUiMessage(R.string.feedback_orders_load_failed, UiMessageType.ERROR)
                    }
                }
        }
    }

    private fun setupFilters() = with(binding) {
        chipWaiting.setOnClickListener {
            selectedFilter = KitchenFilter.WAITING
            renderSelectedFilter()
        }
        chipPreparing.setOnClickListener {
            selectedFilter = KitchenFilter.PREPARING
            renderSelectedFilter()
        }
        renderSelectedFilter()
    }

    private fun renderSelectedFilter() = with(binding) {
        val waitingOrders = allOrders.filter { it.status == KitchenOrderStatus.WAITING }
        val preparingOrders = allOrders.filter { it.status == KitchenOrderStatus.PREPARING }
        val selectedOrders = when (selectedFilter) {
            KitchenFilter.WAITING -> waitingOrders
            KitchenFilter.PREPARING -> preparingOrders
        }

        Log.d(
            TAG,
            "loaded=${allOrders.size} waiting=${waitingOrders.size} preparing=${preparingOrders.size} selected=${selectedOrders.size}",
        )
        adapter.submitList(selectedOrders.toList())
        rvKitchenOrders.isVisible = selectedOrders.isNotEmpty()
        tvEmptyOrders.isVisible = selectedOrders.isEmpty()
        bindChip(chipWaiting, selectedFilter == KitchenFilter.WAITING, waitingOrders.size)
        bindChip(chipPreparing, selectedFilter == KitchenFilter.PREPARING, preparingOrders.size)
    }

    private fun bindChip(chip: TextView, selected: Boolean, count: Int) {
        val baseText = if (chip.id == R.id.chipWaiting) {
            getString(R.string.kitchen_chip_waiting)
        } else {
            getString(R.string.kitchen_chip_preparing)
        }
        chip.text = "$baseText ($count)"
        chip.setBackgroundResource(
            if (selected) R.drawable.bg_chip_selected_gold else R.drawable.bg_chip_unselected_dark,
        )
        chip.setTextColor(
            chip.context.getColor(
                if (selected) R.color.pt_text_dark else R.color.pt_text_primary,
            ),
        )
    }

    private fun setupBottomNav() {
        bindStaffBottomNav(
            root = binding.staffBottomNav.root,
            currentTab = StaffBottomNavTab.KITCHEN,
            onDashboardClick = {
                openStaffDashboard(
                    addToBackStack = false,
                    direction = StaffBottomNavTab.KITCHEN.directionTo(StaffBottomNavTab.DASHBOARD),
                )
            },
            onDeliveryClick = {
                openShipperDeliveryDashboard(
                    addToBackStack = false,
                    direction = StaffBottomNavTab.KITCHEN.directionTo(StaffBottomNavTab.DELIVERY),
                )
            },
            onProfileClick = {
                openCustomerAccount(
                    addToBackStack = false,
                    direction = StaffBottomNavTab.KITCHEN.directionTo(StaffBottomNavTab.PROFILE),
                )
            },
        )
    }

    private fun setupTopBar() {
        bindStaffTopBar(
            root = binding.headerKitchen.root,
            title = getString(R.string.kitchen_default_chef_name),
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
        if (nextStatus != null && canManageKitchenScreen()) {
            updateFirestoreStatus(order.orderId, nextStatus, messageRes)
        } else {
            showUiMessage(R.string.feedback_action_failed, UiMessageType.WARNING)
        }
    }

    private fun updateFirestoreStatus(orderId: String, nextStatus: String, messageRes: Int) {
        KitchenOrderFirestoreRepository.updateOrderStatus(orderId, nextStatus) { result ->
            if (_binding == null || !isAdded) return@updateOrderStatus
            result
                .onSuccess {
                    allOrders = allOrders.map { order ->
                        if (order.orderId == orderId) {
                            order.copy(status = nextStatus.toKitchenOrderStatus())
                        } else {
                            order
                        }
                    }
                    renderSelectedFilter()
                    showUiMessage(messageRes, UiMessageType.SUCCESS)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to update kitchen order to $nextStatus", error)
                    showUiMessage(R.string.feedback_action_failed, UiMessageType.ERROR)
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

    private fun String.toKitchenOrderStatus(): KitchenOrderStatus {
        return when (this) {
            "PREPARING", "BAKING" -> KitchenOrderStatus.PREPARING
            "READY", "READY_FOR_DELIVERY" -> KitchenOrderStatus.READY
            else -> KitchenOrderStatus.WAITING
        }
    }

    override fun onDestroyView() {
        ordersListener?.remove()
        ordersListener = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "KitchenBoard"
    }

}

private enum class KitchenFilter {
    WAITING,
    PREPARING,
}
