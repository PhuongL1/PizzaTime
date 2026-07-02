package com.devpro.pizzatime.feature.shipper.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.databinding.ItemShipperDeliveryOrderBinding

class ShipperDeliveryAdapter(
    private val onStartDeliveryClick: (ShipperDeliveryUiModel) -> Unit,
    private val onItemClick: (ShipperDeliveryUiModel) -> Unit = {},
) : ListAdapter<ShipperDeliveryUiModel, ShipperDeliveryAdapter.ShipperDeliveryViewHolder>(
    ShipperDeliveryDiffCallback,
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShipperDeliveryViewHolder {
        val binding = ItemShipperDeliveryOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ShipperDeliveryViewHolder(binding, onStartDeliveryClick, onItemClick)
    }

    override fun onBindViewHolder(holder: ShipperDeliveryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ShipperDeliveryViewHolder(
        private val binding: ItemShipperDeliveryOrderBinding,
        private val onStartDeliveryClick: (ShipperDeliveryUiModel) -> Unit,
        private val onItemClick: (ShipperDeliveryUiModel) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: ShipperDeliveryUiModel) = with(binding) {
            tvAssignedOrderId.text = order.orderId
            tvAssignedCustomerName.text = order.customerName
            tvAssignedAddress.text = order.address

            tvPaymentBadge.text = if (order.paymentAmount.isNotBlank()) {
                order.paymentAmount
            } else {
                order.paymentLabel
            }

            root.setOnClickListener {
                onItemClick(order)
            }

            btnStartDelivery.setOnClickListener {
                onStartDeliveryClick(order)
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