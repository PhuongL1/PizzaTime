package com.devpro.pizzatime.feature.customer.orderhistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerOrderHistoryBinding
import com.devpro.pizzatime.databinding.ItemCustomerOrderHistoryCardBinding
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.bottomnav.setupCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.topbar.setupCustomerTopBar
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderDetail
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlin.math.roundToInt

class CustomerOrderHistoryFragment : Fragment() {

    private var _binding: FragmentCustomerOrderHistoryBinding? = null
    private val binding: FragmentCustomerOrderHistoryBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerOrderHistoryBinding is only valid between onCreateView and onDestroyView."
        }

    private val historyData: CustomerOrderHistoryUiModel = FakeCustomerOrderHistoryData.getOrderHistory()
    private var selectedFilter: CustomerOrderHistoryFilter = CustomerOrderHistoryFilter.ALL
    private var firestoreOrders: List<CustomerOrderHistoryItemUiModel>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindStaticContent()
        setupFilterActions()
        setupTopBar()
        setupBottomNav()
        renderFilterChips()
        renderOrders()
        renderRewardCard()
        loadFirestoreOrders()
    }

    private fun bindStaticContent() = with(binding) {
        tvTitle.text = historyData.title
        tvSubtitle.text = historyData.subtitle
    }

    private fun setupFilterActions() = with(binding) {
        chipAllOrders.setOnClickListener {
            selectedFilter = CustomerOrderHistoryFilter.ALL
            renderFilterChips()
            renderOrders()
        }

        chipDelivered.setOnClickListener {
            selectedFilter = CustomerOrderHistoryFilter.DELIVERED
            renderFilterChips()
            renderOrders()
        }

        chipCanceled.setOnClickListener {
            selectedFilter = CustomerOrderHistoryFilter.CANCELED
            renderFilterChips()
            renderOrders()
        }
    }

    private fun setupTopBar() = with(binding) {
        setupCustomerTopBar(
            topBar = customerTopBar,
            cartItemCount = 2,
            onCartClick = {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.customer_promo_cart_toast),
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }

    private fun setupBottomNav() = with(binding) {
        setupCustomerBottomNav(
            bottomNav = customerBottomNav,
            selectedTab = CustomerBottomNavTab.ORDERS,
            onCustomerMenuClick = {},
            onCustomerLoyaltyClick = {},
            onCustomerProfileClick = {},
        )
    }

    private fun renderFilterChips() = with(binding) {
        renderChip(chipAllOrders, selectedFilter == CustomerOrderHistoryFilter.ALL)
        renderChip(chipDelivered, selectedFilter == CustomerOrderHistoryFilter.DELIVERED)
        renderChip(chipCanceled, selectedFilter == CustomerOrderHistoryFilter.CANCELED)
    }

    private fun renderChip(view: View, selected: Boolean) {
        view.setBackgroundResource(
            if (selected) {
                R.drawable.bg_customer_order_history_chip_selected
            } else {
                R.drawable.bg_customer_order_history_chip_outline
            },
        )
    }

    private fun renderOrders() = with(binding.orderHistoryContainer) {
        removeAllViews()

        getFilteredOrders().forEach { order ->
            val itemBinding = ItemCustomerOrderHistoryCardBinding.inflate(layoutInflater, this, false)
            bindOrderCard(itemBinding, order)

            addView(
                itemBinding.root,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = 24.dp()
                },
            )
        }
    }

    private fun getFilteredOrders(): List<CustomerOrderHistoryItemUiModel> {
        val orders = firestoreOrders ?: historyData.orders
        return when (selectedFilter) {
            CustomerOrderHistoryFilter.ALL -> orders
            CustomerOrderHistoryFilter.DELIVERED -> orders.filter {
                it.status == CustomerOrderHistoryStatus.DELIVERED
            }
            CustomerOrderHistoryFilter.CANCELED -> orders.filter {
                it.status == CustomerOrderHistoryStatus.CANCELED
            }
        }
    }

    private fun bindOrderCard(
        itemBinding: ItemCustomerOrderHistoryCardBinding,
        order: CustomerOrderHistoryItemUiModel,
    ) = with(itemBinding) {
        tvOrderedAt.text = order.orderedAt
        tvOrderId.text = getString(R.string.customer_order_history_order_id, order.orderId)
        tvStatus.text = order.status.label
        tvTotal.text = formatPrice(order.total)
        tvItemSummary.text = order.itemSummary.joinToString(separator = "\n")

        if (order.imageRes != null) {
            ivOrderImage.setImageResource(order.imageRes)
            tvOrderImagePlaceholder.isVisible = false
        } else {
            ivOrderImage.setImageResource(0)
            tvOrderImagePlaceholder.isVisible = true
        }

        bindStatusStyle(itemBinding, order.status)
        bindOrderActions(itemBinding, order)
    }

    private fun bindStatusStyle(
        itemBinding: ItemCustomerOrderHistoryCardBinding,
        status: CustomerOrderHistoryStatus,
    ) = with(itemBinding) {
        when (status) {
            CustomerOrderHistoryStatus.DELIVERED -> {
                tvStatus.setBackgroundResource(R.drawable.bg_customer_order_history_status_delivered)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.pt_basil_green))
            }
            CustomerOrderHistoryStatus.CANCELED -> {
                tvStatus.setBackgroundResource(R.drawable.bg_customer_order_history_status_canceled)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.pt_copper))
            }
            CustomerOrderHistoryStatus.IN_PROGRESS -> {
                tvStatus.setBackgroundResource(R.drawable.bg_customer_order_history_status_delivered)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.pt_basil_green))
            }
        }
    }

    private fun bindOrderActions(
        itemBinding: ItemCustomerOrderHistoryCardBinding,
        order: CustomerOrderHistoryItemUiModel,
    ) = with(itemBinding) {
        when (order.status) {
            CustomerOrderHistoryStatus.DELIVERED -> {
                btnSecondary.text = getString(R.string.customer_order_history_view_details)
                btnPrimary.text = getString(R.string.customer_order_history_reorder)
                btnPrimary.setBackgroundResource(R.drawable.bg_customer_order_history_primary_button)
                btnSecondary.setBackgroundResource(R.drawable.bg_customer_order_history_outline_button)

                btnSecondary.setOnClickListener { openCustomerOrderDetail(order.orderId) }
                btnPrimary.setOnClickListener { showComingSoonToast(R.string.customer_order_history_reorder_toast) }
            }

            CustomerOrderHistoryStatus.CANCELED -> {
                btnSecondary.text = getString(R.string.customer_order_history_support)
                btnPrimary.text = getString(R.string.customer_order_history_try_again)
                btnPrimary.setBackgroundResource(R.drawable.bg_customer_order_history_outline_button)
                btnSecondary.setBackgroundResource(R.drawable.bg_customer_order_history_muted_outline_button)

                btnSecondary.setOnClickListener { showComingSoonToast(R.string.customer_order_history_support_toast) }
                btnPrimary.setOnClickListener { showComingSoonToast(R.string.customer_order_history_try_again_toast) }
            }

            CustomerOrderHistoryStatus.IN_PROGRESS -> {
                btnSecondary.text = getString(R.string.customer_order_history_view_details)
                btnPrimary.text = getString(R.string.customer_order_history_view_details)
                btnPrimary.setBackgroundResource(R.drawable.bg_customer_order_history_primary_button)
                btnSecondary.setBackgroundResource(R.drawable.bg_customer_order_history_outline_button)

                btnSecondary.setOnClickListener { openCustomerOrderDetail(order.orderId) }
                btnPrimary.setOnClickListener { openCustomerOrderDetail(order.orderId) }
            }
        }
    }

    private fun renderRewardCard() = with(binding) {
        tvRewardTitle.text = historyData.reward.title
        tvRewardDescription.text = historyData.reward.description
        tvRewardCurrentOrders.text = getString(
            R.string.customer_order_history_orders_count,
            historyData.reward.currentOrders,
        )
        tvRewardTargetOrders.text = getString(
            R.string.customer_order_history_orders_count,
            historyData.reward.targetOrders,
        )

        rewardProgress.progress = historyData.reward.currentOrders
        rewardProgress.max = historyData.reward.targetOrders
    }

    private fun loadFirestoreOrders() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CustomerOrderFirestoreRepository.loadOrderHistory(uid) { result ->
            if (!isAdded) return@loadOrderHistory
            result.onSuccess { orders ->
                firestoreOrders = orders
                renderOrders()
            }
        }
    }

    private fun showComingSoonToast(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    private fun formatPrice(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).roundToInt()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}