package com.devpro.pizzatime.feature.admin.orders

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentManageOrdersBinding
import com.devpro.pizzatime.feature.staff.navigation.openStaffOrderDetail
import com.devpro.pizzatime.shared.dialog.AssignShipperDialogFragment
import com.devpro.pizzatime.shared.dialog.CancelOrderConfirmationDialogFragment
import com.devpro.pizzatime.shared.dialog.StatusUpdateConfirmationDialogFragment

class ManageOrdersFragment : Fragment(R.layout.fragment_manage_orders) {

    private var _binding: FragmentManageOrdersBinding? = null
    private val binding: FragmentManageOrdersBinding
        get() = checkNotNull(_binding) {
            "FragmentManageOrdersBinding is only valid between onViewCreated and onDestroyView."
        }

    private val allOrders = FakeAdminOrdersData.getOrders()
    private var selectedStatus = AdminOrderStatus.ALL
    private var searchQuery = ""

    private val adapter = AdminOrderAdapter(
        onViewClick = ::openOrderDetail,
        onAssignClick = ::showAssignShipperDialog,
        onDispatchClick = ::showDispatchConfirmation,
        onCancelClick = ::showCancelOrderDialog,
        onContactClick = ::contactShipper,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentManageOrdersBinding.bind(view)

        setupRecyclerView()
        setupActions()
        setupSearch()
        setupFilters()
        setupAssignShipperResult()
        setupCancelOrderResult()
        setupStatusUpdateResult()
        renderOrders()
    }

    private fun setupRecyclerView() = with(binding.rvOrders) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = this@ManageOrdersFragment.adapter
    }

    private fun setupActions() = with(binding) {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnNewManualOrder.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.manage_orders_manual_order_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    text: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) = Unit

                override fun onTextChanged(
                    text: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    searchQuery = text?.toString()?.trim() ?: ""
                    renderOrders()
                }

                override fun afterTextChanged(editable: Editable?) = Unit
            },
        )
    }

    private fun setupFilters() = with(binding) {
        chipAll.setOnClickListener { selectFilter(AdminOrderStatus.ALL) }
        chipPending.setOnClickListener { selectFilter(AdminOrderStatus.PENDING) }
        chipConfirmed.setOnClickListener { selectFilter(AdminOrderStatus.CONFIRMED) }
        chipReady.setOnClickListener { selectFilter(AdminOrderStatus.READY) }
        chipShipped.setOnClickListener { selectFilter(AdminOrderStatus.SHIPPED) }

        selectFilter(AdminOrderStatus.ALL)
    }

    private fun selectFilter(status: AdminOrderStatus) {
        selectedStatus = status
        updateFilterUi()
        renderOrders()
    }

    private fun updateFilterUi() = with(binding) {
        val chips = mapOf(
            AdminOrderStatus.ALL to chipAll,
            AdminOrderStatus.PENDING to chipPending,
            AdminOrderStatus.CONFIRMED to chipConfirmed,
            AdminOrderStatus.READY to chipReady,
            AdminOrderStatus.SHIPPED to chipShipped,
        )

        chips.forEach { (status, chip) ->
            bindFilterChip(
                chip = chip,
                selected = status == selectedStatus,
            )
        }
    }

    private fun bindFilterChip(
        chip: TextView,
        selected: Boolean,
    ) {
        chip.setBackgroundResource(
            if (selected) {
                R.drawable.bg_manage_orders_filter_selected
            } else {
                R.drawable.bg_manage_orders_filter_unselected
            },
        )

        chip.setTextColor(
            requireContext().getColor(
                if (selected) {
                    R.color.pt_text_dark
                } else {
                    R.color.pt_text_secondary_dark_bg
                },
            ),
        )
    }

    private fun renderOrders() {
        val filteredOrders = allOrders
            .filter { order ->
                selectedStatus == AdminOrderStatus.ALL || order.status == selectedStatus
            }
            .filter { order ->
                searchQuery.isBlank() ||
                        order.orderId.contains(searchQuery, ignoreCase = true) ||
                        order.customerName.contains(searchQuery, ignoreCase = true) ||
                        order.phone.contains(searchQuery, ignoreCase = true)
            }

        adapter.submitList(filteredOrders)
    }

    private fun openOrderDetail(order: AdminOrderUiModel) {
        openStaffOrderDetail(order.orderId)
    }

    private fun showAssignShipperDialog(order: AdminOrderUiModel) {
        AssignShipperDialogFragment
            .newInstance(
                orderId = order.orderId,
                address = getString(R.string.manage_orders_default_address),
            )
            .show(childFragmentManager, ASSIGN_SHIPPER_DIALOG_TAG)
    }

    private fun showDispatchConfirmation(order: AdminOrderUiModel) {
        StatusUpdateConfirmationDialogFragment
            .newInstance(
                orderId = order.orderId,
                fromStatus = order.status.name,
                toStatus = AdminOrderStatus.SHIPPED.name,
                confirmLabel = getString(R.string.manage_orders_dispatch),
            )
            .show(childFragmentManager, STATUS_UPDATE_DIALOG_TAG)
    }

    private fun showCancelOrderDialog(order: AdminOrderUiModel) {
        CancelOrderConfirmationDialogFragment
            .newInstance(
                orderId = order.orderId,
                currentStatus = order.status.name,
            )
            .show(childFragmentManager, CANCEL_ORDER_DIALOG_TAG)
    }

    private fun contactShipper(order: AdminOrderUiModel) {
        Toast.makeText(
            requireContext(),
            getString(R.string.manage_orders_contact_shipper_toast, order.orderId),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun setupAssignShipperResult() {
        childFragmentManager.setFragmentResultListener(
            AssignShipperDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val orderId = bundle.getString(AssignShipperDialogFragment.KEY_ORDER_ID) ?: ""
            val shipperName = bundle.getString(AssignShipperDialogFragment.KEY_SHIPPER_NAME) ?: ""

            Toast.makeText(
                requireContext(),
                getString(R.string.manage_orders_assigned_toast, orderId, shipperName),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupCancelOrderResult() {
        childFragmentManager.setFragmentResultListener(
            CancelOrderConfirmationDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val orderId = bundle.getString(CancelOrderConfirmationDialogFragment.KEY_ORDER_ID) ?: ""

            Toast.makeText(
                requireContext(),
                getString(R.string.manage_orders_cancelled_toast, orderId),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupStatusUpdateResult() {
        childFragmentManager.setFragmentResultListener(
            StatusUpdateConfirmationDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val orderId = bundle.getString(StatusUpdateConfirmationDialogFragment.KEY_ORDER_ID) ?: ""
            val toStatus = bundle.getString(StatusUpdateConfirmationDialogFragment.KEY_TO_STATUS) ?: ""

            Toast.makeText(
                requireContext(),
                getString(R.string.manage_orders_status_updated_toast, orderId, toStatus),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ASSIGN_SHIPPER_DIALOG_TAG = "AssignShipperDialog"
        private const val CANCEL_ORDER_DIALOG_TAG = "CancelOrderConfirmationDialog"
        private const val STATUS_UPDATE_DIALOG_TAG = "StatusUpdateConfirmationDialog"
    }
}