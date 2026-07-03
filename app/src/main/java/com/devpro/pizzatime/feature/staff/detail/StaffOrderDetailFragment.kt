package com.devpro.pizzatime.feature.staff.detail

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentStaffOrderDetailBinding
import com.devpro.pizzatime.feature.staff.StaffOrderFirestoreRepository
import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderStatus
import com.devpro.pizzatime.shared.dialog.AssignShipperDialogFragment
import com.devpro.pizzatime.shared.dialog.CancelOrderConfirmationDialogFragment
import java.util.Locale

class StaffOrderDetailFragment : Fragment(R.layout.fragment_staff_order_detail) {

    private var _binding: FragmentStaffOrderDetailBinding? = null
    private val binding: FragmentStaffOrderDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentStaffOrderDetailBinding is only valid between onViewCreated and onDestroyView."
        }

    private lateinit var currentOrder: StaffOrderDetailUiModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentStaffOrderDetailBinding.bind(view)

        val orderId = arguments?.getString(ARG_ORDER_ID).orEmpty()

        setupCancelOrderResult()
        setupAssignShipperResult()
        loadOrder(orderId)
    }

    private fun loadOrder(orderId: String) {
        if (isFirestoreOrderId(orderId)) {
            StaffOrderFirestoreRepository.loadOrderDetail(orderId) { result ->
                if (!isAdded) return@loadOrderDetail
                val order = result.getOrElse { FakeStaffOrderDetailData.getByOrderId(orderId) }
                bindAndSetup(order)
            }
        } else {
            bindAndSetup(FakeStaffOrderDetailData.getByOrderId(orderId))
        }
    }

    private fun bindAndSetup(order: StaffOrderDetailUiModel) {
        currentOrder = order
        bindOrderDetail(order)
        setupActions()
    }

    private fun isFirestoreOrderId(orderId: String): Boolean {
        return orderId.isNotBlank() && !orderId.startsWith("#") && orderId.length > 8
    }

    private fun bindOrderDetail(order: StaffOrderDetailUiModel) = with(binding) {
        tvOrderId.text = getString(R.string.staff_order_detail_order_title, order.orderId)
        tvReceivedAgo.text = getString(R.string.staff_order_detail_received, order.receivedAgo)
        tvStatus.text = mapStatusText(order.status)

        tvCustomerName.text = order.customerName
        tvCustomerAddress.text = order.deliveryAddress
        tvEstimatedDelivery.text = getString(
            R.string.staff_order_detail_estimated_delivery,
            order.estimatedDeliveryTime,
        )

        tvItemsTotal.text = resources.getQuantityString(
            R.plurals.staff_order_detail_items_total,
            order.itemCount,
            order.itemCount,
        )

        bindMainItem(order.mainItem)
        bindPayment(order)
        bindDeliveryNote(order.deliveryNote)
        bindTimeline(order)
        bindActionVisibility(order.status)
    }

    private fun bindMainItem(item: StaffOrderDetailItemUiModel?) = with(binding) {
        cardMainItem.isVisible = item != null

        if (item == null) {
            return@with
        }

        imgPizza.setImageResource(item.imageRes)
        tvItemName.text = item.name
        tvCrust.text = item.description
        tvSize.text = getString(R.string.staff_order_detail_quantity, item.quantity)
        tvItemPrice.text = formatPrice(item.price)
    }

    private fun bindPayment(order: StaffOrderDetailUiModel) = with(binding) {
        tvPaymentMethod.text = order.paymentMethod
        tvPaymentSummary.text = getString(
            R.string.staff_order_detail_payment_summary,
            formatPrice(order.paymentTotal),
        )
    }

    private fun bindDeliveryNote(note: String) = with(binding) {
        cardRequest.isVisible = note.isNotBlank()
        tvCustomerRequest.text = getString(R.string.staff_order_detail_quoted_note, note)
    }

    private fun bindTimeline(order: StaffOrderDetailUiModel) = with(binding) {
        tvTimelinePlaced.text = getString(
            R.string.staff_order_detail_timeline_order_placed_dynamic,
            order.timeline.orderPlacedTime,
        )

        tvTimelineConfirmed.text = getString(
            R.string.staff_order_detail_timeline_confirmed_dynamic,
            order.timeline.confirmedTime ?: getString(R.string.staff_order_detail_timeline_waiting),
        )

        tvTimelinePreparing.text = getString(
            R.string.staff_order_detail_timeline_preparing_dynamic,
            order.timeline.preparingTime ?: getString(R.string.staff_order_detail_timeline_waiting),
        )

        tvTimelineReady.text = if (order.status == StaffOrderStatus.READY) {
            getString(R.string.staff_order_detail_timeline_ready_now)
        } else {
            getString(R.string.staff_order_detail_timeline_ready_waiting)
        }
    }

    private fun bindActionVisibility(status: StaffOrderStatus) = with(binding) {
        btnAssignShipper.isVisible = status == StaffOrderStatus.READY
        btnDelay10.isVisible = status == StaffOrderStatus.PENDING ||
            status == StaffOrderStatus.CONFIRMED ||
            status == StaffOrderStatus.PREPARING
        btnCancelOrder.isVisible = status == StaffOrderStatus.PENDING || status == StaffOrderStatus.CONFIRMED
    }

    private fun setupActions() = with(binding) {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnCallCustomer.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_order_detail_call_customer_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnDelay10.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_order_detail_delay_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnAssignShipper.setOnClickListener {
            showAssignShipperDialog()
        }

        btnCancelOrder.setOnClickListener {
            showCancelOrderDialog()
        }
    }

    private fun setupCancelOrderResult() {
        childFragmentManager.setFragmentResultListener(
            CancelOrderConfirmationDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val orderId = bundle.getString(CancelOrderConfirmationDialogFragment.KEY_ORDER_ID).orEmpty()
            val reason = bundle.getString(CancelOrderConfirmationDialogFragment.KEY_CANCEL_REASON).orEmpty()

            cancelOrder(orderId, reason)
        }
    }

    private fun setupAssignShipperResult() {
        childFragmentManager.setFragmentResultListener(
            AssignShipperDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val orderId = bundle.getString(AssignShipperDialogFragment.KEY_ORDER_ID).orEmpty()
            val shipperName = bundle.getString(AssignShipperDialogFragment.KEY_SHIPPER_NAME).orEmpty()

            assignShipper(orderId, shipperName)
        }
    }

    private fun showCancelOrderDialog() {
        CancelOrderConfirmationDialogFragment
            .newInstance(
                orderId = currentOrder.orderId,
                currentStatus = binding.tvStatus.text.toString(),
            )
            .show(childFragmentManager, CANCEL_ORDER_DIALOG_TAG)
    }

    private fun showAssignShipperDialog() {
        AssignShipperDialogFragment
            .newInstance(
                orderId = currentOrder.orderId,
                address = currentOrder.deliveryAddress,
            )
            .show(childFragmentManager, ASSIGN_SHIPPER_DIALOG_TAG)
    }

    private fun cancelOrder(orderId: String, reason: String) = with(binding) {
        StaffOrderFirestoreRepository.cancelOrder(orderId) { result ->
            if (!isAdded) return@cancelOrder
            result
                .onSuccess {
                    tvStatus.text = ORDER_STATUS_CANCELLED
                    btnCancelOrder.isVisible = false
                    btnAssignShipper.isVisible = false
                    btnDelay10.isVisible = false

                    val message = if (reason.isBlank()) {
                        getString(R.string.staff_order_detail_cancelled_toast, orderId)
                    } else {
                        getString(R.string.staff_order_detail_cancelled_with_reason_toast, orderId, reason)
                    }

                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        R.string.staff_order_detail_cancel_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    private fun assignShipper(orderId: String, shipperName: String) = with(binding) {
        btnAssignShipper.isVisible = false
        btnDelay10.isVisible = false

        Toast.makeText(
            requireContext(),
            getString(R.string.staff_order_detail_assigned_to_shipper_toast, orderId, shipperName),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun mapStatusText(status: StaffOrderStatus): String {
        return when (status) {
            StaffOrderStatus.PENDING -> getString(R.string.staff_order_detail_preparation_pending)
            StaffOrderStatus.CONFIRMED -> getString(R.string.staff_order_detail_order_confirmed)
            StaffOrderStatus.PREPARING -> getString(R.string.staff_order_detail_preparing)
            StaffOrderStatus.READY -> getString(R.string.staff_order_detail_ready)
            StaffOrderStatus.CANCELLED -> ORDER_STATUS_CANCELLED
        }
    }

    private fun formatPrice(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ORDER_ID = "orderId"

        private const val CANCEL_ORDER_DIALOG_TAG = "CancelOrderConfirmationDialog"
        private const val ASSIGN_SHIPPER_DIALOG_TAG = "AssignShipperDialog"
        private const val ORDER_STATUS_CANCELLED = "CANCELLED"

        fun newInstance(orderId: String): StaffOrderDetailFragment {
            return StaffOrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}
