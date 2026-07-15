package com.devpro.pizzatime.feature.customer.orderhistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.databinding.FragmentCustomerOrderHistoryBinding
import com.devpro.pizzatime.databinding.ItemCustomerOrderHistoryCardBinding
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerTopBar
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.devpro.pizzatime.feature.staff.navigation.openOrderTracking
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlin.math.roundToInt

class CustomerOrderHistoryFragment : Fragment() {

    private var _binding: FragmentCustomerOrderHistoryBinding? = null
    private val binding: FragmentCustomerOrderHistoryBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerOrderHistoryBinding is only valid between onCreateView and onDestroyView."
        }

    private var selectedFilter: CustomerOrderHistoryFilter = CustomerOrderHistoryFilter.ALL
    private var firestoreOrders: List<CustomerOrderHistoryItemUiModel> = emptyList()

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

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            setupTopBar()
        }
    }

    private fun bindStaticContent() = with(binding) {
        tvTitle.setText(R.string.customer_order_history_title)
        tvSubtitle.setText(R.string.customer_order_history_subtitle)
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
        bindCustomerTopBar(
            root = customerTopBar.root,
            cartItemCount = CartStore.items.sumOf { item -> item.quantity },
            onCartClick = { openCartScreen() },
        )
    }

    private fun setupBottomNav() = with(binding) {
        bindCustomerBottomNav(
            root = customerBottomNav.root,
            selectedTab = CustomerBottomNavTab.ORDERS,
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
        return when (selectedFilter) {
            CustomerOrderHistoryFilter.ALL -> firestoreOrders
            CustomerOrderHistoryFilter.DELIVERED -> firestoreOrders.filter {
                it.status == CustomerOrderHistoryStatus.DELIVERED
            }
            CustomerOrderHistoryFilter.CANCELED -> firestoreOrders.filter {
                it.status == CustomerOrderHistoryStatus.CANCELED
            }
        }
    }

    private fun bindOrderCard(
        itemBinding: ItemCustomerOrderHistoryCardBinding,
        order: CustomerOrderHistoryItemUiModel,
    ) = with(itemBinding) {
        tvOrderedAt.text = order.orderedAt
        tvOrderId.text = getString(
            R.string.customer_order_history_order_id,
            order.displayOrderCode.removePrefix("#"),
        )
        tvStatus.text = order.status.label
        tvTotal.text = formatPrice(order.total)
        tvItemSummary.text = order.itemSummary.joinToString(separator = "\n")
        root.setOnClickListener { openOrderTracking(order.orderId) }

        if (order.imageUrl.isNotBlank()) {
            ivOrderImage.loadProductImage(order.imageUrl, order.imageRes ?: R.drawable.img_pizza_time)
            tvOrderImagePlaceholder.isVisible = false
        } else if (order.imageRes != null) {
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

                btnSecondary.setOnClickListener { openOrderTracking(order.orderId) }
                btnPrimary.setOnClickListener { showUnavailableAction(R.string.customer_order_history_reorder_toast) }
            }

            CustomerOrderHistoryStatus.CANCELED -> {
                btnSecondary.text = getString(R.string.customer_order_history_support)
                btnPrimary.text = getString(R.string.customer_order_history_try_again)
                btnPrimary.setBackgroundResource(R.drawable.bg_customer_order_history_outline_button)
                btnSecondary.setBackgroundResource(R.drawable.bg_customer_order_history_muted_outline_button)

                btnSecondary.setOnClickListener { showUnavailableAction(R.string.customer_order_history_support_toast) }
                btnPrimary.setOnClickListener { showUnavailableAction(R.string.customer_order_history_try_again_toast) }
            }

            CustomerOrderHistoryStatus.IN_PROGRESS -> {
                btnSecondary.text = getString(R.string.customer_order_history_view_details)
                btnPrimary.text = getString(R.string.customer_order_history_view_details)
                btnPrimary.setBackgroundResource(R.drawable.bg_customer_order_history_primary_button)
                btnSecondary.setBackgroundResource(R.drawable.bg_customer_order_history_outline_button)

                btnSecondary.setOnClickListener { openOrderTracking(order.orderId) }
                btnPrimary.setOnClickListener { openOrderTracking(order.orderId) }
            }
        }
    }

    private fun renderRewardCard() = with(binding) {
        val currentOrders = firestoreOrders.size.coerceAtMost(REWARD_TARGET_ORDERS)
        tvRewardTitle.setText(R.string.customer_order_history_reward_title)
        tvRewardDescription.setText(R.string.customer_order_history_reward_description)
        tvRewardCurrentOrders.text = getString(
            R.string.customer_order_history_orders_count,
            currentOrders,
        )
        tvRewardTargetOrders.text = getString(
            R.string.customer_order_history_orders_count,
            REWARD_TARGET_ORDERS,
        )

        rewardProgress.progress = currentOrders
        rewardProgress.max = REWARD_TARGET_ORDERS
    }

    private fun loadFirestoreOrders() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CustomerOrderFirestoreRepository.loadOrderHistory(uid) { result ->
            if (_binding == null || !isAdded) return@loadOrderHistory
            result.onSuccess { orders ->
                firestoreOrders = orders
                renderOrders()
                renderRewardCard()
            }
        }
    }

    private fun showUnavailableAction(messageRes: Int) {
        showUiMessage(messageRes, UiMessageType.INFO)
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

    companion object {
        private const val REWARD_TARGET_ORDERS = 5
    }
}
