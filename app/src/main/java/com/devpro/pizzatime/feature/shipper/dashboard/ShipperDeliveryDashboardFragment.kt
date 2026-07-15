package com.devpro.pizzatime.feature.shipper.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentShipperDeliveryDashboardBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav
import com.devpro.pizzatime.feature.shipper.ShipperOrderFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.bindStaffBottomNav
import com.devpro.pizzatime.feature.staff.navigation.bindStaffTopBar
import com.devpro.pizzatime.feature.staff.navigation.canManageShipperScreen
import com.devpro.pizzatime.feature.staff.navigation.directionTo
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
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
        canManageActions = { canManageShipperScreen() },
    )
    private var ordersListener: ListenerRegistration? = null
    private var latestDashboard = ShipperDashboardUiModel(
        activeOrders = emptyList(),
        deliveredOrders = emptyList(),
        activeOrderCount = 0,
        readyOrderCount = 0,
        completedOrderCount = 0,
        deliveryEarnings = 0.0,
    )
    private var showingHistory = false
    private var hasShownOrdersLoadError = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentShipperDeliveryDashboardBinding.bind(view)

        renderDashboard(latestDashboard)
        setupAssignedDeliveries()
        setupTopBar()
        setupBottomNav()
        listenFirestoreOrders()
    }

    private fun bindActiveDelivery(activeDelivery: ShipperDeliveryUiModel?) = with(binding) {
        activeDeliveryCard.isVisible = activeDelivery != null
        tvActiveDeliveryEmpty.isVisible = activeDelivery == null
        if (activeDelivery == null) {
            btnNavigate.isVisible = false
            btnCallCustomer.isVisible = false
            btnNavigate.setOnClickListener(null)
            btnCallCustomer.setOnClickListener(null)
            return@with
        }

        tvActiveOrderId.text = activeDelivery.displayOrderCode
        tvActiveEta.text = activeDelivery.etaLabel
        tvActiveCustomerName.text = activeDelivery.customerName
        tvActiveAddress.text = activeDelivery.address
        tvActivePaymentLabel.text = activeDelivery.paymentLabel
        tvActivePaymentAmount.text = activeDelivery.paymentAmount

        val canManage = canManageShipperScreen()
        btnNavigate.isVisible = canManage
        btnCallCustomer.isVisible = canManage
        if (canManage) {
            btnNavigate.setOnClickListener {
                openShipperDeliveryDetail(activeDelivery.orderId)
            }

            btnCallCustomer.setOnClickListener {
                showUiMessage(R.string.shipper_message_call_customer, UiMessageType.INFO)
            }
        } else {
            btnNavigate.setOnClickListener(null)
            btnCallCustomer.setOnClickListener(null)
        }
    }

    private fun setupAssignedDeliveries() = with(binding.rvAssignedDeliveries) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = deliveryAdapter
        isNestedScrollingEnabled = false
    }

    private fun listenFirestoreOrders() {
        ordersListener?.remove()
        val shipperId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        ordersListener = ShipperOrderFirestoreRepository.listenDashboard(shipperId) { result ->
            if (_binding == null || !isAdded) return@listenDashboard
            result
                .onSuccess { dashboard ->
                    hasShownOrdersLoadError = false
                    latestDashboard = dashboard
                    renderDashboard(dashboard)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to listen for shipper dashboard", error)
                    if (!hasShownOrdersLoadError) {
                        hasShownOrdersLoadError = true
                        showUiMessage(R.string.feedback_orders_load_failed, UiMessageType.ERROR)
                    }
                }
        }
    }

    private fun renderDashboard(dashboard: ShipperDashboardUiModel) = with(binding) {
        val activeDelivery = dashboard.activeOrders.firstOrNull {
            it.status == ShipperDeliveryStatus.ACTIVE
        } ?: dashboard.activeOrders.firstOrNull()
        bindActiveDelivery(activeDelivery)

        tvActiveOrderCount.text = getString(
            R.string.shipper_active_order_count_format,
            dashboard.activeOrderCount,
        )
        tvReadyCount.text = getString(
            R.string.shipper_ready_count_format,
            dashboard.readyOrderCount,
        )
        tvCompletedCount.text = dashboard.completedOrderCount.toString()
        tvCompletedLabel.setText(R.string.shipper_completed_label)
        tvDeliveryEarnings.text = formatMoney(dashboard.deliveryEarnings)
        tvDeliveryEarningsLabel.setText(R.string.shipper_delivery_earnings_label)

        val listItems = if (showingHistory) dashboard.deliveredOrders else dashboard.activeOrders
        tvAssignedTitle.setText(
            if (showingHistory) {
                R.string.shipper_delivered_history_title
            } else {
                R.string.shipper_active_orders_title
            },
        )
        tvViewHistory.setText(
            if (showingHistory) {
                R.string.shipper_show_active_orders
            } else {
                R.string.shipper_view_delivered_history
            },
        )
        rvAssignedDeliveries.isVisible = listItems.isNotEmpty()
        tvDeliveryListEmpty.isVisible = listItems.isEmpty()
        tvDeliveryListEmpty.setText(
            if (showingHistory) {
                R.string.shipper_no_delivery_history
            } else {
                R.string.shipper_no_active_delivery
            },
        )
        deliveryAdapter.submitList(listItems)
    }

    private fun setupBottomNav() {
        if (FakeSessionStore.currentRole == UserRole.ADMIN) {
            bindAdminBottomNav(
                root = binding.staffBottomNav.root,
                selectedDestination = AdminBottomNavDestination.SHIPPER,
                onDashboardClick = { openAdminDashboard() },
                onManageMenuClick = { openManageMenu() },
                onManagePromoCodesClick = { openShipperDeliveryDashboard() },
                onManageStaffClick = { openCustomerAccount() },
            )
        } else {
            bindStaffBottomNav(
                root = binding.staffBottomNav.root,
                currentTab = StaffBottomNavTab.DELIVERY,
                onDashboardClick = {
                    openStaffDashboard(
                        addToBackStack = false,
                        direction = StaffBottomNavTab.DELIVERY.directionTo(StaffBottomNavTab.DASHBOARD),
                    )
                },
                onKitchenClick = {
                    openKitchenBoard(
                        addToBackStack = false,
                        direction = StaffBottomNavTab.DELIVERY.directionTo(StaffBottomNavTab.KITCHEN),
                    )
                },
                onProfileClick = {
                    openCustomerAccount(
                        addToBackStack = false,
                        direction = StaffBottomNavTab.DELIVERY.directionTo(StaffBottomNavTab.PROFILE),
                    )
                },
            )
        }

        binding.tvViewHistory.setOnClickListener {
            showingHistory = !showingHistory
            renderDashboard(latestDashboard)
        }
    }

    private fun setupTopBar() {
        bindStaffTopBar(
            root = binding.headerShipper.root,
            title = getString(R.string.staff_nav_delivery),
        )
    }

    private fun startDelivery(order: ShipperDeliveryUiModel) {
        if (!canManageShipperScreen()) return
        openShipperDeliveryDetail(order.orderId)
    }

    private fun formatMoney(value: Double): String {
        return String.format(java.util.Locale.US, "$%.2f", value)
    }

    override fun onDestroyView() {
        ordersListener?.remove()
        ordersListener = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val TAG = "ShipperDashboard"
    }
}
