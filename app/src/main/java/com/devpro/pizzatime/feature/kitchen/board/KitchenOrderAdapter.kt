package com.devpro.pizzatime.feature.kitchen.board

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.ItemKitchenOrderBinding
import com.devpro.pizzatime.databinding.ItemKitchenOrderFoodBinding

class KitchenOrderAdapter(
    private val onPrimaryActionClick: (KitchenOrderUiModel) -> Unit,
    private val onItemClick: (KitchenOrderUiModel) -> Unit = {},
) : ListAdapter<KitchenOrderUiModel, KitchenOrderAdapter.KitchenOrderViewHolder>(
    KitchenOrderDiffCallback,
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KitchenOrderViewHolder {
        val binding = ItemKitchenOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return KitchenOrderViewHolder(binding, onPrimaryActionClick, onItemClick)
    }

    override fun onBindViewHolder(holder: KitchenOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class KitchenOrderViewHolder(
        private val binding: ItemKitchenOrderBinding,
        private val onPrimaryActionClick: (KitchenOrderUiModel) -> Unit,
        private val onItemClick: (KitchenOrderUiModel) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: KitchenOrderUiModel) = with(binding) {
            tvOrderType.text = order.fulfillmentLabel
            tvOrderId.text = order.displayOrderCode
            tvOrderTime.text = order.timeLabel

            bindFoodItems(order)
            bindStatusStyle(order)

            root.setOnClickListener {
                onItemClick(order)
            }

            btnPrimaryAction.setOnClickListener {
                onPrimaryActionClick(order)
            }
        }

        private fun bindFoodItems(order: KitchenOrderUiModel) = with(binding) {
            llOrderItems.removeAllViews()

            val shouldShowFoodItems = order.status == KitchenOrderStatus.WAITING ||
                    order.status == KitchenOrderStatus.PREPARING

            llOrderItems.visibility = if (shouldShowFoodItems) View.VISIBLE else View.GONE
            layoutReadyPanel.visibility = if (order.status == KitchenOrderStatus.READY) View.VISIBLE else View.GONE
            layoutPendingReview.visibility = if (order.status == KitchenOrderStatus.NEW) View.VISIBLE else View.GONE

            tvReadyNote.text = order.note.orEmpty()

            if (!shouldShowFoodItems) return@with

            order.items.forEachIndexed { index, food ->
                val foodBinding = ItemKitchenOrderFoodBinding.inflate(
                    LayoutInflater.from(llOrderItems.context),
                    llOrderItems,
                    false,
                )

                bindFoodItem(
                    foodBinding = foodBinding,
                    food = food,
                    isLastItem = index == order.items.lastIndex,
                )

                llOrderItems.addView(foodBinding.root)
            }
        }

        private fun bindFoodItem(
            foodBinding: ItemKitchenOrderFoodBinding,
            food: KitchenOrderItemUiModel,
            isLastItem: Boolean,
        ) = with(foodBinding) {
            val context = root.context

            tvFoodName.text = context.getString(
                R.string.kitchen_food_quantity_name,
                food.quantity,
                food.name,
            )
            tvFoodSize.text = food.sizeLabel

            tvFoodModifier.text = food.modifier.orEmpty()
            tvFoodModifier.visibility = if (food.modifier.isNullOrBlank()) View.GONE else View.VISIBLE

            tvFoodCrust.text = food.crust.orEmpty()
            tvFoodCrust.visibility = if (food.crust.isNullOrBlank()) View.GONE else View.VISIBLE

            foodDetailContainer.visibility = if (
                food.modifier.isNullOrBlank() &&
                food.crust.isNullOrBlank()
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }

            viewFoodDivider.visibility = if (isLastItem) View.GONE else View.VISIBLE
        }

        private fun bindStatusStyle(order: KitchenOrderUiModel) = with(binding) {
            val context = root.context

            when (order.status) {
                KitchenOrderStatus.WAITING -> {
                    kitchenOrderRoot.setBackgroundResource(R.drawable.bg_kitchen_card_dark)
                    viewTopAccent.setBackgroundColor(context.color(R.color.pt_status_pending))
                    tvOrderType.setTextColor(context.color(R.color.pt_gold_light))
                    tvOrderTime.setTextColor(context.color(R.color.pt_gold_light))

                    tvOrderStatusLabel.visibility = View.VISIBLE
                    tvOrderStatusLabel.text = context.getString(R.string.kitchen_status_priority)

                    btnPrimaryAction.setBackgroundResource(R.drawable.bg_button_primary_gold)
                    btnPrimaryAction.setTextColor(context.color(R.color.pt_text_dark))
                    btnPrimaryAction.text = context.getString(R.string.kitchen_action_start_baking)
                }

                KitchenOrderStatus.PREPARING -> {
                    kitchenOrderRoot.setBackgroundResource(R.drawable.bg_kitchen_card_dark)
                    viewTopAccent.setBackgroundColor(context.color(R.color.pt_status_preparing))
                    tvOrderType.setTextColor(context.color(R.color.pt_status_preparing))
                    tvOrderTime.setTextColor(context.color(R.color.pt_status_preparing))

                    tvOrderStatusLabel.visibility = View.VISIBLE
                    tvOrderStatusLabel.text = context.getString(R.string.kitchen_status_in_oven)

                    val progress = order.progressLabel
                        ?: context.getString(R.string.kitchen_default_progress)

                    btnPrimaryAction.setBackgroundResource(R.drawable.bg_kitchen_action_pink)
                    btnPrimaryAction.setTextColor(context.color(R.color.pt_danger_button_text))
                    btnPrimaryAction.text = context.getString(
                        R.string.kitchen_action_progress,
                        progress,
                    )
                }

                KitchenOrderStatus.READY -> {
                    kitchenOrderRoot.setBackgroundResource(R.drawable.bg_kitchen_card_ready)
                    viewTopAccent.setBackgroundColor(context.color(R.color.pt_status_ready))
                    tvOrderType.setTextColor(context.color(R.color.pt_status_ready))

                    tvOrderTime.text = context.getString(R.string.kitchen_ready_check)
                    tvOrderTime.setTextColor(context.color(R.color.pt_status_ready))

                    tvOrderStatusLabel.visibility = View.GONE

                    btnPrimaryAction.setBackgroundResource(R.drawable.bg_kitchen_action_green_outline)
                    btnPrimaryAction.setTextColor(context.color(R.color.pt_status_ready))
                    btnPrimaryAction.text = context.getString(R.string.kitchen_action_handed_over)
                }

                KitchenOrderStatus.NEW -> {
                    kitchenOrderRoot.setBackgroundResource(R.drawable.bg_kitchen_card_new)
                    viewTopAccent.setBackgroundColor(context.color(R.color.pt_text_muted))
                    tvOrderType.setTextColor(context.color(R.color.pt_text_secondary))
                    tvOrderTime.setTextColor(context.color(R.color.pt_text_secondary))

                    tvOrderStatusLabel.visibility = View.GONE

                    btnPrimaryAction.setBackgroundResource(R.drawable.bg_kitchen_action_disabled)
                    btnPrimaryAction.setTextColor(context.color(R.color.pt_text_primary))
                    btnPrimaryAction.text = context.getString(R.string.kitchen_action_accept_order)
                }
            }
        }

        private fun android.content.Context.color(colorRes: Int): Int {
            return ContextCompat.getColor(this, colorRes)
        }
    }
}

private object KitchenOrderDiffCallback : DiffUtil.ItemCallback<KitchenOrderUiModel>() {

    override fun areItemsTheSame(
        oldItem: KitchenOrderUiModel,
        newItem: KitchenOrderUiModel,
    ): Boolean = oldItem.orderId == newItem.orderId

    override fun areContentsTheSame(
        oldItem: KitchenOrderUiModel,
        newItem: KitchenOrderUiModel,
    ): Boolean = oldItem == newItem
}
