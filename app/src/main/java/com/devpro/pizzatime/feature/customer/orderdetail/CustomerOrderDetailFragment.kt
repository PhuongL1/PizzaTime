package com.devpro.pizzatime.feature.customer.orderdetail

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerOrderDetailBinding
import com.devpro.pizzatime.databinding.ItemCustomerOrderDetailLineBinding
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerTopBar
import com.devpro.pizzatime.feature.customer.orderhistory.CustomerOrderFirestoreRepository
import java.util.Locale

class CustomerOrderDetailFragment : Fragment() {

    private var _binding: FragmentCustomerOrderDetailBinding? = null
    private val binding: FragmentCustomerOrderDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerOrderDetailBinding is only valid between onCreateView and onDestroyView."
        }
    private var currentOrderId: String = ""
    private var isCancellingOrder = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTopBar()
        setupBottomNav()
        setupActions()
        val orderId = arguments?.getString(ARG_ORDER_ID).orEmpty()
        loadOrder(orderId)
    }

    private fun setupTopBar() = with(binding) {
        bindCustomerTopBar(
            root = customerTopBar.root,
            cartItemCount = 0,
        )
    }

    private fun setupBottomNav() = with(binding) {
        bindCustomerBottomNav(
            root = bottomNav.root,
            selectedTab = CustomerBottomNavTab.ORDERS,
        )
    }

    private fun loadOrder(orderId: String) {
        currentOrderId = orderId
        if (isFirestoreOrderId(orderId)) {
            CustomerOrderFirestoreRepository.loadOrderDetail(orderId) { result ->
                if (_binding == null || !isAdded) return@loadOrderDetail
                val detail = result.getOrElse {
                    FakeCustomerOrderDetailData.getOrderDetail(orderId.ifBlank { DEFAULT_ORDER_ID })
                }
                bindOrderDetail(detail)
            }
        } else {
            bindOrderDetail(
                FakeCustomerOrderDetailData.getOrderDetail(orderId.ifBlank { DEFAULT_ORDER_ID })
            )
        }
    }

    private fun isFirestoreOrderId(orderId: String): Boolean =
        orderId.isNotBlank() && !orderId.startsWith("#") && orderId.length > 8

    private fun bindOrderDetail(detail: CustomerOrderDetailUiModel) = with(binding) {
        tvStatus.text = detail.statusLabel
        tvOrderId.text = getString(R.string.customer_order_detail_order_id, detail.orderId)
        tvOrderTime.text = detail.orderTime
        ivHeroImage.setImageResource(detail.heroImageRes)
        tvHeroMessage.text = detail.heroMessage

        bindItems(detail.items)
        bindBill(detail.bill)
        bindAddress(detail)
        bindStatusHistory(detail.statusHistory)
        btnCancelOrder.isVisible = detail.canCancel && isFirestoreOrderId(detail.orderId)
    }

    private fun bindItems(items: List<CustomerOrderItemUiModel>) = with(binding.orderItemsContainer) {
        removeAllViews()

        items.forEachIndexed { index, item ->
            val itemBinding = ItemCustomerOrderDetailLineBinding.inflate(layoutInflater, this, false)

            itemBinding.tvItemName.text = getString(
                R.string.customer_order_detail_item_name,
                item.quantity,
                item.name,
            )
            itemBinding.tvItemDescription.text = item.description
            itemBinding.tvItemPrice.text = formatPrice(item.price)

            if (item.imageRes != null) {
                itemBinding.ivItemImage.setImageResource(item.imageRes)
                itemBinding.tvItemPlaceholder.isVisible = false
            } else {
                itemBinding.ivItemImage.setImageDrawable(null)
                itemBinding.tvItemPlaceholder.isVisible = true
            }

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                if (index > 0) {
                    topMargin = 14.dp
                }
            }

            addView(itemBinding.root)
        }
    }

    private fun bindBill(bill: CustomerBillUiModel) = with(binding) {
        tvSubtotalValue.text = formatPrice(bill.subtotal)
        tvDeliveryFeeValue.text = formatPrice(bill.deliveryFee)
        tvTaxesValue.text = formatPrice(bill.taxes)
        tvDiscountLabel.text = bill.discountLabel
        tvDiscountValue.text = formatSignedPrice(bill.discount)
        tvTotalAmount.text = formatPrice(bill.total)
    }

    private fun bindAddress(detail: CustomerOrderDetailUiModel) = with(binding) {
        tvDeliveredTo.text = detail.deliveryAddressTitle
        tvAddressLine1.text = detail.deliveryAddressLine1
        tvAddressLine2.text = detail.deliveryAddressLine2
        tvStoreName.text = detail.storeName
        tvPickupAddress.text = detail.pickupAddress
        tvStorePhone.text = detail.storePhone
    }

    private fun bindStatusHistory(items: List<CustomerOrderStatusHistoryUiModel>) = with(binding) {
        statusHistoryCard.isVisible = items.isNotEmpty()
        statusHistoryContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            statusHistoryContainer.addView(
                createStatusHistoryRow(
                    item = item,
                    isLast = index == items.lastIndex,
                ),
            )
        }
    }

    private fun setupActions() = with(binding) {
        btnReorder.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.customer_order_detail_reorder_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnSupport.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.customer_order_detail_support_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnCancelOrder.setOnClickListener {
            showCancelOrderDialog()
        }
    }

    private fun showCancelOrderDialog() {
        if (!isFirestoreOrderId(currentOrderId)) {
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.customer_order_detail_cancel_title)
            .setMessage(R.string.customer_order_detail_cancel_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.customer_order_detail_cancel_confirm) { _, _ ->
                cancelOrder()
            }
            .show()
    }

    private fun cancelOrder() {
        if (isCancellingOrder) {
            return
        }

        isCancellingOrder = true
        binding.btnCancelOrder.isEnabled = false
        CustomerOrderFirestoreRepository.cancelOrder(currentOrderId) { result ->
            if (_binding == null || !isAdded) return@cancelOrder
            result
                .onSuccess {
                    isCancellingOrder = false
                    Toast.makeText(
                        requireContext(),
                        R.string.customer_order_detail_cancel_success,
                        Toast.LENGTH_SHORT,
                    ).show()
                    loadOrder(currentOrderId)
                }
                .onFailure { error ->
                    isCancellingOrder = false
                    binding.btnCancelOrder.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        error.message ?: getString(R.string.customer_order_detail_cancel_failed),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    private fun formatPrice(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

    private fun formatSignedPrice(value: Double): String {
        return if (value < 0) {
            String.format(Locale.US, "-$%.2f", kotlin.math.abs(value))
        } else {
            String.format(Locale.US, "$%.2f", value)
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun createStatusHistoryRow(
        item: CustomerOrderStatusHistoryUiModel,
        isLast: Boolean,
    ): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val indicator = TextView(requireContext()).apply {
            text = "•"
            setTextColor(requireContext().getColor(R.color.pt_gold))
            textSize = 22f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginEnd = 14.dp
            }
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            )
        }

        val title = TextView(requireContext()).apply {
            text = formatStatusLabel(item.status)
            setTextColor(requireContext().getColor(R.color.pt_text_primary_dark_bg))
            textSize = 17f
        }

        val meta = TextView(requireContext()).apply {
            text = buildHistoryMeta(item)
            setTextColor(requireContext().getColor(R.color.pt_text_secondary_dark_bg))
            textSize = 13f
        }

        val note = TextView(requireContext()).apply {
            text = item.note
            setTextColor(requireContext().getColor(R.color.pt_text_secondary_dark_bg))
            textSize = 15f
            isVisible = item.note.isNotBlank()
        }

        content.addView(title)
        content.addView(meta)
        content.addView(note)

        row.addView(indicator)
        row.addView(content)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        if (!isLast) {
            params.bottomMargin = 18.dp
        }
        row.layoutParams = params
        return row
    }

    private fun formatStatusLabel(status: String): String {
        return when (status.uppercase(Locale.US)) {
            "PENDING" -> "Order Placed"
            "CONFIRMED" -> "Confirmed"
            "PREPARING" -> "Preparing"
            "BAKING" -> "Baking"
            "READY" -> "Ready"
            "ASSIGNED_TO_SHIPPER" -> "Assigned to Shipper"
            "DELIVERING" -> "Out for Delivery"
            "DELIVERED" -> "Delivered"
            "CANCELLED" -> "Cancelled"
            else -> status.ifBlank { "Unknown" }
        }
    }

    private fun formatActorRole(role: String): String {
        return when (role.uppercase(Locale.US)) {
            "CUSTOMER" -> "Customer"
            "STAFF" -> "Staff"
            "KITCHEN" -> "Kitchen"
            "SHIPPER" -> "Shipper"
            "ADMIN" -> "Admin"
            else -> role.ifBlank { "System" }
        }
    }

    private fun buildHistoryMeta(item: CustomerOrderStatusHistoryUiModel): String {
        val actor = formatActorRole(item.actorRole)
        return if (item.timeText.isBlank()) actor else "$actor · ${item.timeText}"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ORDER_ID = "arg_order_id"
        private const val DEFAULT_ORDER_ID = "PT-9821"

        fun newInstance(orderId: String): CustomerOrderDetailFragment {
            return CustomerOrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}
