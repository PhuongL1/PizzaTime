package com.devpro.pizzatime.feature.customer.orderdetail

import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentCustomerOrderDetailBinding
import com.devpro.pizzatime.databinding.ItemCustomerOrderDetailLineBinding
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.navigation.bindPizzaFlowTopBar
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.customer.orderhistory.CustomerOrderFirestoreRepository
import com.devpro.pizzatime.feature.customer.rating.CustomerProductReviewFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class CustomerOrderDetailFragment : Fragment() {

    private var _binding: FragmentCustomerOrderDetailBinding? = null
    private val binding: FragmentCustomerOrderDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerOrderDetailBinding is only valid between onCreateView and onDestroyView."
        }

    private var currentOrderId: String = ""
    private var currentOrderDetail: CustomerOrderDetailUiModel? = null
    private var existingRatings: Map<String, Int> = emptyMap()
    private var isRealOrderLoaded = false
    private var isCancellingOrder = false
    private var isLoadingRatings = false
    private var isSubmittingRatings = false

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

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            setupTopBar()
        }
    }

    private fun setupTopBar() = with(binding) {
        bindPizzaFlowTopBar(
            root = customerTopBar.root,
            cartItemCount = CartStore.items.sumOf { item -> item.quantity },
            onBackClick = { parentFragmentManager.popBackStack() },
            onCartClick = { openCartScreen() },
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
        existingRatings = emptyMap()
        isRealOrderLoaded = false
        if (isFirestoreOrderId(orderId)) {
            CustomerOrderFirestoreRepository.loadOrderDetail(orderId) { result ->
                if (_binding == null || !isAdded) return@loadOrderDetail
                result
                    .onSuccess { detail ->
                        isRealOrderLoaded = true
                        currentOrderDetail = detail
                        bindOrderDetail(detail)
                        loadRatingState(detail)
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to load customer orderId=$orderId", error)
                        AppUiMessageBus.publish(
                            textRes = if (error.message?.contains("not found", ignoreCase = true) == true) {
                                R.string.notification_order_unavailable
                            } else {
                                R.string.customer_order_detail_load_failed
                            },
                            type = UiMessageType.ERROR,
                        )
                        parentFragmentManager.popBackStack()
                    }
            }
        } else {
            val detail = FakeCustomerOrderDetailData.getOrderDetail(orderId.ifBlank { DEFAULT_ORDER_ID })
            isRealOrderLoaded = false
            currentOrderDetail = detail
            bindOrderDetail(detail)
        }
    }

    private fun isFirestoreOrderId(orderId: String): Boolean =
        orderId.matches(ORDER_CODE_KEY_REGEX) ||
            (orderId.isNotBlank() && !orderId.startsWith("#") && orderId.length > 8)

    private fun bindOrderDetail(detail: CustomerOrderDetailUiModel) = with(binding) {
        tvStatus.text = detail.statusLabel
        tvOrderId.text = getString(
            R.string.customer_order_detail_order_id,
            detail.displayOrderCode.removePrefix("#"),
        )
        tvOrderTime.text = detail.orderTime
        tvHeroMessage.text = detail.heroMessage
        detailImage(detail)

        bindItems(detail.items)
        bindBill(detail)
        bindAddress(detail)
        bindStatusHistory(detail.statusHistory)

        val delivered = detail.statusLabel.uppercase(Locale.US) == "DELIVERED"
        btnRateOrder.isVisible = delivered && isRealOrderLoaded
        btnRateOrder.text = if (existingRatings.isNotEmpty()) {
            getString(R.string.update_rating)
        } else {
            getString(R.string.rate_order)
        }

        btnCancelOrder.isVisible = detail.canCancel && isFirestoreOrderId(detail.orderId)
    }

    private fun detailImage(detail: CustomerOrderDetailUiModel) = with(binding) {
        ivHeroImage.loadProductImage(detail.heroImageUrl, detail.heroImageRes)
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

            if (item.imageUrl.isNotBlank()) {
                itemBinding.ivItemImage.loadProductImage(item.imageUrl, item.imageRes ?: R.drawable.img_pizza_time)
                itemBinding.tvItemPlaceholder.isVisible = false
            } else if (item.imageRes != null) {
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

    private fun bindBill(detail: CustomerOrderDetailUiModel) = with(binding) {
        val bill = detail.bill
        tvSubtotalValue.text = formatPrice(bill.subtotal)
        tvDeliveryFeeValue.text = formatPrice(bill.deliveryFee)
        tvDeliveryDistanceValue.text = detail.distanceKm?.let { formatDistance(it) }
            ?: getString(R.string.common_not_provided)
        tvTaxesValue.text = formatPrice(bill.taxes)
        tvPaymentMethodValue.text = detail.paymentMethod.ifBlank { getString(R.string.payment_method_cash_on_delivery) }
        tvPaymentStatusValue.text = detail.paymentStatus.ifBlank { getString(R.string.payment_status_unpaid) }
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
            showUiMessage(R.string.customer_order_detail_reorder_message, UiMessageType.INFO)
        }

        btnSupport.setOnClickListener {
            showUiMessage(R.string.customer_order_detail_support_message, UiMessageType.INFO)
        }

        btnRateOrder.setOnClickListener {
            showRatingDialog()
        }

        btnCancelOrder.setOnClickListener {
            showCancelOrderDialog()
        }
    }

    private fun loadRatingState(detail: CustomerOrderDetailUiModel) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank() || detail.items.isEmpty() || !isRealOrderLoaded) {
            existingRatings = emptyMap()
            binding.btnRateOrder.text = getString(R.string.rate_order)
            return
        }

        if (isLoadingRatings) return
        isLoadingRatings = true
        CustomerProductReviewFirestoreRepository.loadOrderRatings(
            orderId = detail.orderId,
            customerId = uid,
        ) { result ->
            isLoadingRatings = false
            if (_binding == null) return@loadOrderRatings
            result.onSuccess { ratings ->
                existingRatings = ratings
                binding.btnRateOrder.text = if (ratings.isNotEmpty()) {
                    getString(R.string.update_rating)
                } else {
                    getString(R.string.rate_order)
                }
            }
        }
    }

    private fun showRatingDialog() {
        val detail = currentOrderDetail ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            showUiMessage(R.string.customer_favorites_login_required, UiMessageType.WARNING)
            return
        }
        if (!isRealOrderLoaded) {
            showUiMessage(R.string.could_not_save_rating, UiMessageType.ERROR)
            return
        }

        if (detail.statusLabel.uppercase(Locale.US) != "DELIVERED") {
            return
        }
        val rateableItems = detail.items.distinctBy { item ->
            item.productId.trim().ifBlank { item.name.trim().lowercase(Locale.US) }
        }
        if (rateableItems.isEmpty()) {
            showUiMessage(R.string.select_a_rating, UiMessageType.WARNING)
            return
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp, 20.dp, 24.dp, 8.dp)
            background = ColorDrawable(requireContext().getColor(R.color.pt_surface_dark))
        }

        val selectedRatings = rateableItems.associate { item ->
            item.productId to (existingRatings[item.productId] ?: 0)
        }.toMutableMap()
        var updateSaveButtonState: (() -> Unit)? = null

        rateableItems.forEachIndexed { index, item ->
            val title = TextView(requireContext()).apply {
                text = item.name
                setTextColor(requireContext().getColor(R.color.pt_cream))
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                if (index > 0) {
                    setPadding(0, 20.dp, 0, 0)
                }
            }

            val subtitle = TextView(requireContext()).apply {
                text = item.description
                setTextColor(requireContext().getColor(R.color.pt_text_primary_dark_bg))
                alpha = 0.9f
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 6.dp, 0, 10.dp)
                isVisible = item.description.isNotBlank()
            }

            container.addView(title)
            if (item.description.isNotBlank()) {
                container.addView(subtitle)
            }
            container.addView(
                createStarRow(
                    selectedRating = selectedRatings[item.productId] ?: 0,
                ) { rating ->
                    selectedRatings[item.productId] = rating
                    updateSaveButtonState?.invoke()
                },
            )
        }

        val content = ScrollView(requireContext()).apply {
            background = ColorDrawable(requireContext().getColor(R.color.pt_surface_dark))
            addView(container)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.rate_order)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save_rating, null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(
                ColorDrawable(requireContext().getColor(R.color.pt_surface_dark)),
            )
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            saveButton.setTextColor(requireContext().getColor(R.color.pt_copper))
            cancelButton.setTextColor(requireContext().getColor(R.color.pt_text_primary_dark_bg))
            updateSaveButtonState = {
                val hasAllRatings = rateableItems.isNotEmpty() && rateableItems.all {
                    (selectedRatings[it.productId] ?: 0) in 1..5
                }
                saveButton.isEnabled = hasAllRatings
                saveButton.alpha = if (hasAllRatings) 1f else 0.5f
            }

            updateSaveButtonState?.invoke()
            saveButton.setOnClickListener {
                if (isSubmittingRatings) return@setOnClickListener
                val payload = rateableItems.associate { item ->
                    item.productId to (selectedRatings[item.productId] ?: 0)
                }.filterValues { it in 1..5 }
                if (payload.size != rateableItems.size) {
                    showUiMessage(R.string.select_a_rating, UiMessageType.WARNING)
                    return@setOnClickListener
                }

                isSubmittingRatings = true
                saveButton.isEnabled = false
                CustomerProductReviewFirestoreRepository.submitOrderRatings(
                    orderId = detail.orderId,
                    customerId = uid,
                    ratings = payload,
                ) { result ->
                    isSubmittingRatings = false
                    if (_binding == null) return@submitOrderRatings
                    result
                        .onSuccess {
                            existingRatings = payload
                            binding.btnRateOrder.text = getString(R.string.update_rating)
                            showUiMessage(R.string.rating_saved, UiMessageType.SUCCESS)
                            dialog.dismiss()
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Submit rating failed", error)
                            showUiMessage(R.string.could_not_save_rating, UiMessageType.ERROR)
                            updateSaveButtonState?.invoke()
                        }
                }
            }
        }

        dialog.show()
    }

    private fun createStarRow(
        selectedRating: Int,
        onRatingSelected: (Int) -> Unit,
    ): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 2.dp, 0, 2.dp)
        }

        repeat(5) { index ->
            val starNumber = index + 1
            row.addView(
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(52.dp, 52.dp).apply {
                        if (index > 0) {
                            marginStart = 6.dp
                        }
                    }
                    gravity = android.view.Gravity.CENTER
                    text = STAR_CHAR
                    textSize = 30f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(
                        requireContext().getColor(
                            if (starNumber <= selectedRating) R.color.pt_gold else R.color.pt_text_primary_dark_bg,
                        ),
                    )
                    alpha = if (starNumber <= selectedRating) 1f else 0.66f
                    setOnClickListener {
                        onRatingSelected(starNumber)
                        updateStarRow(row, starNumber)
                    }
                },
            )
        }

        updateStarRow(row, selectedRating)
        return row
    }

    private fun updateStarRow(row: LinearLayout, selectedRating: Int) {
        for (index in 0 until row.childCount) {
            val star = row.getChildAt(index) as? TextView ?: continue
            val starNumber = index + 1
            star.setTextColor(
                requireContext().getColor(
                    if (starNumber <= selectedRating) R.color.pt_gold else R.color.pt_text_primary_dark_bg,
                ),
            )
            star.alpha = if (starNumber <= selectedRating) 1f else 0.66f
        }
    }

    private fun showCancelOrderDialog() {
        if (!isFirestoreOrderId(currentOrderId)) {
            return
        }

        MaterialAlertDialogBuilder(requireContext())
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
                    showUiMessage(R.string.customer_order_detail_cancel_success, UiMessageType.SUCCESS)
                    loadOrder(currentOrderId)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to cancel customer orderId=$currentOrderId", error)
                    isCancellingOrder = false
                    binding.btnCancelOrder.isEnabled = true
                    showUiMessage(R.string.customer_order_detail_cancel_failed, UiMessageType.ERROR)
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

    private fun formatDistance(distanceKm: Double): String {
        return String.format(Locale.US, "%.1f km", distanceKm)
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
        private const val TAG = "CustomerOrderDetail"
        private const val ARG_ORDER_ID = "arg_order_id"
        private const val DEFAULT_ORDER_ID = "PT-9821"
        private val ORDER_CODE_KEY_REGEX = Regex("[a-z]{2}-\\d{4}")
        private const val STAR_CHAR = "★"

        fun newInstance(orderId: String): CustomerOrderDetailFragment {
            return CustomerOrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}
