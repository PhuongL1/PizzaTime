package com.devpro.pizzatime.feature.customer.notifications

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.ItemCustomerNotificationBinding

class CustomerNotificationAdapter(
    private val onNotificationClick: (CustomerNotificationUiModel) -> Unit,
) : RecyclerView.Adapter<CustomerNotificationAdapter.NotificationViewHolder>() {

    private val items = mutableListOf<CustomerNotificationUiModel>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<CustomerNotificationUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): NotificationViewHolder {
        val binding = ItemCustomerNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int,
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class NotificationViewHolder(
        private val binding: ItemCustomerNotificationBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: CustomerNotificationUiModel) = with(binding) {
            root.setBackgroundResource(
                if (notification.isUnread) {
                    R.drawable.bg_customer_notifications_card_unread
                } else {
                    R.drawable.bg_customer_notifications_card
                },
            )
            unreadIndicator.isVisible = notification.isUnread
            iconContainer.setBackgroundResource(notification.iconBackgroundRes)
            ivIcon.setImageResource(notification.iconRes)
            ivIcon.setColorFilter(
                ContextCompat.getColor(root.context, notification.iconTintRes),
            )
            tvTitle.text = notification.title
            tvMessage.text = notification.body
            tvTime.text = notification.timestampLabel
            root.setOnClickListener { onNotificationClick(notification) }
        }
    }
}
