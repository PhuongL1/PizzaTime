package com.devpro.pizzatime.feature.customer.notifications

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
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

            tvIcon.text = getIcon(notification.type)
            tvIcon.setBackgroundResource(getIconBackground(notification.type))
            tvIcon.setTextColor(root.context.getColor(getIconColor(notification.type)))

            tvTitle.text = notification.title
            tvMessage.text = notification.message
            tvTime.text = notification.timeLabel
            tvTime.setTextColor(root.context.getColor(getTimeColor(notification.type, notification.isUnread)))

            root.setOnClickListener {
                onNotificationClick(notification)
            }
        }

        private fun getIcon(type: CustomerNotificationType): String {
            return when (type) {
                CustomerNotificationType.DELIVERY -> "⌖"
                CustomerNotificationType.PROMO -> "□"
                CustomerNotificationType.WEATHER -> "△"
                CustomerNotificationType.ORDER -> "♨"
                CustomerNotificationType.LOYALTY -> "▱"
            }
        }

        private fun getIconBackground(type: CustomerNotificationType): Int {
            return when (type) {
                CustomerNotificationType.DELIVERY -> R.drawable.bg_customer_notifications_icon_warm
                CustomerNotificationType.WEATHER -> R.drawable.bg_customer_notifications_icon_alert
                CustomerNotificationType.PROMO,
                CustomerNotificationType.ORDER,
                CustomerNotificationType.LOYALTY,
                    -> R.drawable.bg_customer_notifications_icon_neutral
            }
        }

        private fun getIconColor(type: CustomerNotificationType): Int {
            return when (type) {
                CustomerNotificationType.DELIVERY -> R.color.pt_copper
                CustomerNotificationType.WEATHER -> R.color.pt_danger_text
                CustomerNotificationType.PROMO,
                CustomerNotificationType.ORDER,
                CustomerNotificationType.LOYALTY,
                    -> R.color.pt_border_warm
            }
        }

        private fun getTimeColor(
            type: CustomerNotificationType,
            unread: Boolean,
        ): Int {
            return when {
                type == CustomerNotificationType.WEATHER -> R.color.pt_danger_text
                unread -> R.color.pt_copper
                else -> R.color.pt_border_warm
            }
        }
    }
}