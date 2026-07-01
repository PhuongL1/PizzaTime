package com.devpro.pizzatime.feature.admin.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.ItemAdminOrderBinding
import java.util.Locale

class AdminOrderAdapter(
    private val onViewClick: (AdminOrderUiModel) -> Unit,
    private val onAssignClick: (AdminOrderUiModel) -> Unit,
    private val onDispatchClick: (AdminOrderUiModel) -> Unit,
    private val onCancelClick: (AdminOrderUiModel) -> Unit,
    private val onContactClick: (AdminOrderUiModel) -> Unit,
) : RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder>() {

    private val items = mutableListOf<AdminOrderUiModel>()

    fun submitList(newItems: List<AdminOrderUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AdminOrderViewHolder {
        val binding = ItemAdminOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AdminOrderViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AdminOrderViewHolder,
        position: Int,
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AdminOrderViewHolder(
        private val binding: ItemAdminOrderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: AdminOrderUiModel) = with(binding) {
            tvOrderId.text = root.context.getString(
                R.string.manage_orders_order_id_format,
                order.orderId,
            )
            tvCustomerName.text = order.customerName
            tvItemsSummary.text = order.itemsSummary
            tvTotalValue.text = formatPrice(order.total)
            tvMetaText.text = order.metaText

            bindStatus(order)
            bindActions(order)
        }

        private fun bindStatus(order: AdminOrderUiModel) = with(binding) {
            tvStatus.text = when (order.status) {
                AdminOrderStatus.PENDING -> root.context.getString(R.string.manage_orders_status_pending)
                AdminOrderStatus.CONFIRMED -> root.context.getString(R.string.manage_orders_status_confirmed)
                AdminOrderStatus.READY -> root.context.getString(R.string.manage_orders_status_ready)
                AdminOrderStatus.SHIPPED -> root.context.getString(R.string.manage_orders_status_shipped)
                AdminOrderStatus.DELIVERED -> root.context.getString(R.string.manage_orders_status_delivered)
                AdminOrderStatus.CANCELLED -> root.context.getString(R.string.manage_orders_status_cancelled)
                AdminOrderStatus.ALL -> root.context.getString(R.string.manage_orders_filter_all)
            }

            tvStatus.setBackgroundResource(
                when (order.status) {
                    AdminOrderStatus.PENDING -> R.drawable.bg_manage_orders_status_pending
                    AdminOrderStatus.CONFIRMED -> R.drawable.bg_manage_orders_status_confirmed
                    AdminOrderStatus.READY -> R.drawable.bg_manage_orders_status_ready
                    AdminOrderStatus.SHIPPED,
                    AdminOrderStatus.DELIVERED,
                        -> R.drawable.bg_manage_orders_status_shipped
                    AdminOrderStatus.CANCELLED,
                    AdminOrderStatus.ALL,
                        -> R.drawable.bg_manage_orders_status_confirmed
                },
            )
        }

        private fun bindActions(order: AdminOrderUiModel) = with(binding) {
            btnPrimaryAction.text = when (order.status) {
                AdminOrderStatus.READY -> root.context.getString(R.string.manage_orders_dispatch)
                AdminOrderStatus.SHIPPED -> root.context.getString(R.string.manage_orders_contact)
                else -> root.context.getString(R.string.manage_orders_assign)
            }

            btnPrimaryAction.setBackgroundResource(
                if (order.status == AdminOrderStatus.READY) {
                    R.drawable.bg_manage_orders_action_primary
                } else {
                    R.drawable.bg_manage_orders_action_button
                },
            )

            btnView.setOnClickListener {
                onViewClick(order)
            }

            btnPrimaryAction.setOnClickListener {
                when (order.status) {
                    AdminOrderStatus.READY -> onDispatchClick(order)
                    AdminOrderStatus.SHIPPED -> onContactClick(order)
                    else -> onAssignClick(order)
                }
            }

            btnCancel.setOnClickListener {
                onCancelClick(order)
            }
        }

        private fun formatPrice(value: Double): String {
            return String.format(Locale.US, "$%.2f", value)
        }
    }
}