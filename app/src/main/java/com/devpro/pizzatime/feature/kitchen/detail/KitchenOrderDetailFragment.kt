package com.devpro.pizzatime.feature.kitchen.detail

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentKitchenOrderDetailBinding
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
        currentOrder = FakeKitchenOrderDetailData.getOrderDetail(orderId)

        bindOrder(currentOrder)
        setupActions()
        setupStatusUpdateResult()
        setupCancelOrderResult()
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
        val hasAllergy = order.allergyTitle != null || order.allergyMessage != null
        allergyCard.isVisible = hasAllergy

        if (!hasAllergy) return@with

        tvAllergyTitle.text = order.allergyTitle.orEmpty()
        tvAllergyMessage.text = order.allergyMessage.orEmpty()
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

            updateStatus(KitchenOrderDetailStatus.CANCELLED)

            Toast.makeText(
                requireContext(),
                getString(R.string.kitchen_order_detail_cancelled_toast, orderId),
                Toast.LENGTH_SHORT,
            ).show()
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
        FakeKitchenOrderDetailData.updateStatus(currentOrder.orderId, status)
        currentOrder = currentOrder.copy(status = status)

        binding.tvStatus.text = mapStatusText(status)
        bindActionState(status)

        Toast.makeText(
            requireContext(),
            getString(R.string.kitchen_order_detail_status_updated_toast, mapStatusText(status)),
            Toast.LENGTH_SHORT,
        ).show()
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
