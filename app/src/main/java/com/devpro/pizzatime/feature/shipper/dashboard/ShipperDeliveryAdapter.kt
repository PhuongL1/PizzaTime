package com.devpro.pizzatime.feature.shipper.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.ItemShipperDeliveryOrderBinding

class ShipperDeliveryAdapter(
    private val onStartDeliveryClick: (ShipperDeliveryUiModel) -> Unit,
    private val onItemClick: (ShipperDeliveryUiModel) -> Unit = {},
    private val canManageActions: () -> Boolean = { true },
) : ListAdapter<ShipperDeliveryUiModel, ShipperDeliveryAdapter.ShipperDeliveryViewHolder>(
    ShipperDeliveryDiffCallback,
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShipperDeliveryViewHolder {
        val binding = ItemShipperDeliveryOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ShipperDeliveryViewHolder(binding, onStartDeliveryClick, onItemClick, canManageActions)
    }

    override fun onBindViewHolder(holder: ShipperDeliveryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ShipperDeliveryViewHolder(
        private val binding: ItemShipperDeliveryOrderBinding,
        private val onStartDeliveryClick: (ShipperDeliveryUiModel) -> Unit,
        private val onItemClick: (ShipperDeliveryUiModel) -> Unit,
        private val canManageActions: () -> Boolean,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: ShipperDeliveryUiModel) = with(binding) {
            tvAssignedOrderId.text = order.displayOrderCode
            tvAssignedCustomerName.text = order.customerName
            tvAssignedAddress.text = order.address
            tvAssignedMeta.isVisible = order.etaLabel.isNotBlank() || order.paymentLabel.isNotBlank()
            tvAssignedMeta.text = listOf(order.etaLabel, order.paymentLabel)
                .filter { it.isNotBlank() }
                .joinToString(" • ")

            tvPaymentBadge.text = if (order.paymentAmount.isNotBlank()) {
                order.paymentAmount
            } else {
                order.paymentLabel
            }
            val canManage = canManageActions()
            btnStartDelivery.isVisible = canManage
            btnStartDelivery.text = when (order.status) {
                ShipperDeliveryStatus.ACTIVE ->
                    root.context.getString(R.string.shipper_detail_complete_delivery)
                ShipperDeliveryStatus.DELIVERED ->
                    root.context.getString(R.string.staff_action_view_details)
                ShipperDeliveryStatus.ASSIGNED ->
                    root.context.getString(R.string.shipper_detail_start_delivery)
            }

            root.setOnClickListener {
                onItemClick(order)
            }

            if (canManage) {
                btnStartDelivery.setOnClickListener {
                    onStartDeliveryClick(order)
                }
            } else {
                btnStartDelivery.setOnClickListener(null)
            }
        }
    }
}

private object ShipperDeliveryDiffCallback : DiffUtil.ItemCallback<ShipperDeliveryUiModel>() {

    override fun areItemsTheSame(
        oldItem: ShipperDeliveryUiModel,
        newItem: ShipperDeliveryUiModel,
    ): Boolean = oldItem.orderId == newItem.orderId

    override fun areContentsTheSame(
        oldItem: ShipperDeliveryUiModel,
        newItem: ShipperDeliveryUiModel,
    ): Boolean = oldItem == newItem
}
