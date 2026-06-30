package com.devpro.pizzatime.feature.admin.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.databinding.ItemAdminRecentOrderBinding

class AdminRecentOrderAdapter(
    private val onOrderClick: (AdminRecentOrderUiModel) -> Unit,
) : ListAdapter<AdminRecentOrderUiModel, AdminRecentOrderAdapter.AdminRecentOrderViewHolder>(
    AdminRecentOrderDiffCallback,
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminRecentOrderViewHolder {
        val binding = ItemAdminRecentOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AdminRecentOrderViewHolder(binding, onOrderClick)
    }

    override fun onBindViewHolder(holder: AdminRecentOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AdminRecentOrderViewHolder(
        private val binding: ItemAdminRecentOrderBinding,
        private val onOrderClick: (AdminRecentOrderUiModel) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: AdminRecentOrderUiModel) = with(binding) {
            tvRecentOrderId.text = order.orderId
            tvRecentOrderSummary.text = order.summary
            tvRecentOrderPrice.text = order.price

            root.setOnClickListener {
                onOrderClick(order)
            }
        }
    }
}

private object AdminRecentOrderDiffCallback : DiffUtil.ItemCallback<AdminRecentOrderUiModel>() {

    override fun areItemsTheSame(
        oldItem: AdminRecentOrderUiModel,
        newItem: AdminRecentOrderUiModel,
    ): Boolean = oldItem.orderId == newItem.orderId

    override fun areContentsTheSame(
        oldItem: AdminRecentOrderUiModel,
        newItem: AdminRecentOrderUiModel,
    ): Boolean = oldItem == newItem
}