package com.devpro.pizzatime.feature.kitchen.detail

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentKitchenOrderDetailBinding
import com.devpro.pizzatime.feature.kitchen.board.KitchenOrderFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.canManageKitchenScreen
import com.devpro.pizzatime.shared.dialog.CancelOrderConfirmationDialogFragment
import com.devpro.pizzatime.shared.dialog.StatusUpdateConfirmationDialogFragment

class KitchenOrderDetailFragment : Fragment(R.layout.fragment_kitchen_order_detail) {

    private var _binding: FragmentKitchenOrderDetailBinding? = null
    private val binding: FragmentKitchenOrderDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentKitchenOrderDetailBinding is only valid between onViewCreated and onDestroyView."
        }

    private lateinit var currentOrder: KitchenOrderDetailUiModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentKitchenOrderDetailBinding.bind(view)

        val orderId = arguments?.getString(ARG_ORDER_ID).orEmpty()
        setupActions()
        setupStatusUpdateResult()
        setupCancelOrderResult()
        bindActionState(KitchenOrderDetailStatus.READY)
        loadOrder(orderId)
    }

    private fun loadOrder(orderId: String) {
        KitchenOrderFirestoreRepository.loadOrderDetail(orderId) { result ->
            if (_binding == null) return@loadOrderDetail
            result
                .onSuccess { order ->
                    currentOrder = order
                    bindOrder(order)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load kitchen orderId=$orderId", error)
                    AppUiMessageBus.publish(
                        R.string.notification_order_unavailable,
                        UiMessageType.ERROR,
                    )
                    parentFragmentManager.popBackStack()
                }
        }
    }

    private fun bindOrder(order: KitchenOrderDetailUiModel) = with(binding) {
        tvOrderId.text = getString(
            R.string.kitchen_order_detail_order_id_format,
            order.displayOrderCode.removePrefix("#"),
        )
        tvReceivedAgo.text = getString(R.string.kitchen_order_detail_received_ago, order.receivedAgo)
        tvStatus.text = mapStatusText(order.status)

        tvItemName.text = order.item.name
        tvSizeValue.text = order.item.size
        tvCrustValue.text = order.item.crust
        ivItemImage.setImageResource(order.item.imageRes)

        bindToppings(order.item.toppings)
        bindAllergy(order)
        bindCustomerRequest(order)
        bindActionState(order.status)
    }

    private fun bindToppings(toppings: List<String>) {
        val toppingViews = listOf(
            binding.tvTopping1,
            binding.tvTopping2,
            binding.tvTopping3,
            binding.tvTopping4,
            binding.tvTopping5,
        )

        toppingViews.forEachIndexed { index, textView ->
            val topping = toppings.getOrNull(index)
            textView.isVisible = topping != null
            textView.text = topping?.let {
                getString(R.string.kitchen_order_detail_topping_line, it)
            }.orEmpty()
        }
    }

    private fun bindAllergy(order: KitchenOrderDetailUiModel) = with(binding) {
        allergyCard.isVisible = true
        tvAllergyTitle.text = order.allergyTitle ?: getString(R.string.kitchen_order_detail_no_value)
        tvAllergyMessage.text = order.allergyMessage ?: getString(R.string.kitchen_order_detail_no_value)
    }

    private fun bindCustomerRequest(order: KitchenOrderDetailUiModel) = with(binding) {
        tvCustomerRequest.text = getString(
            R.string.kitchen_order_detail_customer_request_format,
            order.customerRequest,
        )

        bindTags(order.tags)
    }

    private fun bindTags(tags: List<String>) {
        val tagViews = listOf(binding.tvTagPrimary, binding.tvTagSecondary)

        tagViews.forEachIndexed { index, textView ->
            val tag = tags.getOrNull(index)
            textView.isVisible = tag != null
            textView.text = tag.orEmpty()
        }
    }

    private fun bindActionState(status: KitchenOrderDetailStatus) = with(binding) {
        val canManage = canManageKitchenScreen()
        tvReadOnlyIndicator.isVisible = !canManage
        if (!canManage) {
            btnStartPreparing.isVisible = false
            btnStartBaking.isVisible = false
            btnMarkReady.isVisible = false
            btnCancelOrder.isVisible = false
            return@with
        }

        btnStartPreparing.isVisible = status == KitchenOrderDetailStatus.PENDING
        btnStartBaking.isVisible = status == KitchenOrderDetailStatus.PREPARING
        btnMarkReady.isVisible = status == KitchenOrderDetailStatus.BAKING
        btnCancelOrder.isVisible = status != KitchenOrderDetailStatus.READY &&
                status != KitchenOrderDetailStatus.CANCELLED
    }

    private fun setupActions() = with(binding) {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (!canManageKitchenScreen()) {
            btnStartPreparing.setOnClickListener(null)
            btnStartBaking.setOnClickListener(null)
            btnMarkReady.setOnClickListener(null)
            btnCancelOrder.setOnClickListener(null)
            return@with
        }

        btnStartPreparing.setOnClickListener {
            showStatusUpdateDialog(
                fromStatus = KitchenOrderDetailStatus.PENDING,
                toStatus = KitchenOrderDetailStatus.PREPARING,
                confirmLabel = getString(R.string.kitchen_order_detail_start_preparing),
            )
        }

        btnStartBaking.setOnClickListener {
            showStatusUpdateDialog(
                fromStatus = KitchenOrderDetailStatus.PREPARING,
                toStatus = KitchenOrderDetailStatus.BAKING,
                confirmLabel = getString(R.string.kitchen_order_detail_start_baking),
            )
        }

        btnMarkReady.setOnClickListener {
            showStatusUpdateDialog(
                fromStatus = KitchenOrderDetailStatus.BAKING,
                toStatus = KitchenOrderDetailStatus.READY,
                confirmLabel = getString(R.string.kitchen_order_detail_mark_ready),
            )
        }

        btnCancelOrder.setOnClickListener {
            showCancelOrderDialog()
        }
    }

    private fun setupStatusUpdateResult() {
        childFragmentManager.setFragmentResultListener(
            StatusUpdateConfirmationDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val toStatus = bundle
                .getString(StatusUpdateConfirmationDialogFragment.KEY_TO_STATUS)
                .orEmpty()
            val newStatus = toKitchenStatus(toStatus)

            updateStatus(newStatus)
        }
    }

    private fun setupCancelOrderResult() {
        childFragmentManager.setFragmentResultListener(
            CancelOrderConfirmationDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val orderId = bundle
                .getString(CancelOrderConfirmationDialogFragment.KEY_ORDER_ID)
                .orEmpty()
            val reason = bundle
                .getString(CancelOrderConfirmationDialogFragment.KEY_CANCEL_REASON)
                .orEmpty()

            cancelOrder(orderId, reason)
        }
    }

    private fun showStatusUpdateDialog(
        fromStatus: KitchenOrderDetailStatus,
        toStatus: KitchenOrderDetailStatus,
        confirmLabel: String,
    ) {
        StatusUpdateConfirmationDialogFragment
            .newInstance(
                orderId = currentOrder.orderId,
                fromStatus = fromStatus.name,
                toStatus = toStatus.name,
                confirmLabel = confirmLabel,
            )
            .show(childFragmentManager, STATUS_UPDATE_DIALOG_TAG)
    }

    private fun showCancelOrderDialog() {
        CancelOrderConfirmationDialogFragment
            .newInstance(
                orderId = currentOrder.orderId,
                currentStatus = currentOrder.status.name,
            )
            .show(childFragmentManager, CANCEL_ORDER_DIALOG_TAG)
    }

    private fun updateStatus(status: KitchenOrderDetailStatus) {
        if (!canManageKitchenScreen()) return

        val firestoreStatus = when (status) {
            KitchenOrderDetailStatus.PREPARING -> "PREPARING"
            KitchenOrderDetailStatus.BAKING -> "BAKING"
            KitchenOrderDetailStatus.READY -> "READY_FOR_DELIVERY"
            else -> null
        }
        if (firestoreStatus == null) {
            showUiMessage(R.string.feedback_action_failed, UiMessageType.WARNING)
            return
        }

        KitchenOrderFirestoreRepository.updateOrderStatus(
            orderId = currentOrder.orderId,
            newStatus = firestoreStatus,
        ) { result ->
            if (_binding == null) return@updateOrderStatus
            result
                .onSuccess {
                    currentOrder = currentOrder.copy(status = status)
                    binding.tvStatus.text = mapStatusText(status)
                    bindActionState(status)
                    showUiMessage(
                        textRes = R.string.kitchen_order_detail_status_updated_message,
                        type = UiMessageType.SUCCESS,
                        args = listOf(mapStatusText(status)),
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to update kitchen orderId=${currentOrder.orderId}", error)
                    showUiMessage(R.string.kitchen_order_detail_update_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun cancelOrder(
        orderId: String,
        reason: String,
    ) {
        if (!canManageKitchenScreen()) return

        binding.btnCancelOrder.isEnabled = false
        KitchenOrderFirestoreRepository.cancelOrder(orderId, reason) { result ->
            if (_binding == null || !isAdded) return@cancelOrder
            result
                .onSuccess {
                    currentOrder = currentOrder.copy(status = KitchenOrderDetailStatus.CANCELLED)
                    binding.tvStatus.text = mapStatusText(KitchenOrderDetailStatus.CANCELLED)
                    bindActionState(KitchenOrderDetailStatus.CANCELLED)
                    AppUiMessageBus.publish(
                        textRes = R.string.kitchen_order_detail_cancelled_message,
                        type = UiMessageType.SUCCESS,
                        args = listOf(orderId),
                    )
                    parentFragmentManager.popBackStack()
                }
                .onFailure { error ->
                    Log.e(TAG, "Could not cancel kitchen order $orderId", error)
                    binding.btnCancelOrder.isEnabled = true
                    showUiMessage(R.string.kitchen_order_detail_cancel_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun toKitchenStatus(status: String): KitchenOrderDetailStatus {
        return KitchenOrderDetailStatus.entries.firstOrNull { it.name == status }
            ?: currentOrder.status
    }

    private fun mapStatusText(status: KitchenOrderDetailStatus): String {
        return when (status) {
            KitchenOrderDetailStatus.PENDING -> getString(R.string.kitchen_order_detail_status_pending)
            KitchenOrderDetailStatus.PREPARING -> getString(R.string.kitchen_order_detail_status_preparing)
            KitchenOrderDetailStatus.BAKING -> getString(R.string.kitchen_order_detail_status_baking)
            KitchenOrderDetailStatus.READY -> getString(R.string.kitchen_order_detail_status_ready)
            KitchenOrderDetailStatus.CANCELLED -> getString(R.string.kitchen_order_detail_status_cancelled)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ORDER_ID = "orderId"

        private const val TAG = "KitchenOrderDetail"
        private const val STATUS_UPDATE_DIALOG_TAG = "StatusUpdateConfirmationDialog"
        private const val CANCEL_ORDER_DIALOG_TAG = "CancelOrderConfirmationDialog"

        fun newInstance(orderId: String): KitchenOrderDetailFragment {
            return KitchenOrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}
