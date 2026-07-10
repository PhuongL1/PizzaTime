package com.devpro.pizzatime.feature.staff.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.ItemStaffOrderBinding

class StaffOrderAdapter(
    private val onConfirmClick: (StaffOrderUiModel) -> Unit,
    private val onDetailClick: (StaffOrderUiModel) -> Unit,
    private val canManageActions: () -> Boolean = { true },
) : ListAdapter<StaffOrderUiModel, StaffOrderAdapter.StaffOrderViewHolder>(StaffOrderDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffOrderViewHolder {
        val binding = ItemStaffOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return StaffOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StaffOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StaffOrderViewHolder(
        private val binding: ItemStaffOrderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StaffOrderUiModel) = with(binding) {
            tvOrderId.text = item.displayOrderCode
            tvCustomerName.text = item.customerName
            tvTimeAgo.text = item.timeAgo
            tvOrderSummary.text = item.orderSummary
            tvPrice.text = item.price

            bindFulfillment(item.fulfillmentType)
            bindAction(item)
        }

        private fun bindFulfillment(type: StaffFulfillmentType) = with(binding.tvFulfillment) {
            when (type) {
                StaffFulfillmentType.DELIVERY -> {
                    setText(R.string.staff_fulfillment_delivery)
                    setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_delivery,
                        0,
                        0,
                        0,
                    )
                }

                StaffFulfillmentType.COLLECTION -> {
                    setText(R.string.staff_fulfillment_collection)
                    setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_collect,
                        0,
                        0,
                        0,
                    )
                }
            }
        }

        private fun bindAction(item: StaffOrderUiModel) = with(binding) {
            if (!canManageActions()) {
                btnViewDetails.isVisible = false
                btnPrimaryAction.isVisible = false
                btnPrimaryAction.setOnClickListener(null)
                btnViewDetails.setOnClickListener(null)
                return@with
            }

            btnPrimaryAction.isVisible = true
            val isPending = item.status == StaffOrderStatus.PENDING

            btnViewDetails.isVisible = isPending

            if (isPending) {
                btnPrimaryAction.setText(R.string.staff_action_confirm_order)
                btnPrimaryAction.setBackgroundResource(R.drawable.bg_button_primary_gold)
                btnPrimaryAction.setTextColor("#3A210D".toColorInt())
            } else {
                btnPrimaryAction.setText(R.string.staff_action_view_details)
                btnPrimaryAction.setBackgroundResource(R.drawable.bg_button_outline_gold)
                btnPrimaryAction.setTextColor("#CF843F".toColorInt())
            }

            btnPrimaryAction.setOnClickListener {
                if (isPending) {
                    onConfirmClick(item)
                } else {
                    onDetailClick(item)
                }
            }

            btnViewDetails.setOnClickListener {
                onDetailClick(item)
            }
        }
    }

    private object StaffOrderDiffCallback : DiffUtil.ItemCallback<StaffOrderUiModel>() {

        override fun areItemsTheSame(
            oldItem: StaffOrderUiModel,
            newItem: StaffOrderUiModel,
        ): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(
            oldItem: StaffOrderUiModel,
            newItem: StaffOrderUiModel,
        ): Boolean {
            return oldItem == newItem
        }
    }
}
