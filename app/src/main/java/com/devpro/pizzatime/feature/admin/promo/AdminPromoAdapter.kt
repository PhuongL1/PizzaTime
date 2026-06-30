package com.devpro.pizzatime.feature.admin.promo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.ItemAdminPromoBinding

class AdminPromoAdapter(
    private val onEditClick: (AdminPromoUiModel) -> Unit,
    private val onDeleteClick: (AdminPromoUiModel) -> Unit,
    private val onShareClick: (AdminPromoUiModel) -> Unit,
    private val onReactivateClick: (AdminPromoUiModel) -> Unit,
) : ListAdapter<AdminPromoUiModel, AdminPromoAdapter.AdminPromoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminPromoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAdminPromoBinding.inflate(inflater, parent, false)
        return AdminPromoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminPromoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminPromoViewHolder(
        private val binding: ItemAdminPromoBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AdminPromoUiModel) = with(binding) {
            rootPromoCard.setBackgroundResource(
                if (item.isHighlighted) {
                    R.drawable.bg_promo_card_highlight
                } else {
                    R.drawable.bg_promo_card
                },
            )

            tvCode.text = item.code
            tvTitle.text = item.title
            bindStatus(item)

            bindField(layoutDiscount, tvDiscountValue, item.discountText)
            bindField(layoutExpiry, tvExpiryValue, item.expiryText)
            bindField(layoutMinSpend, tvMinSpendValue, item.minSpendText)
            bindField(layoutEndsIn, tvEndsInValue, item.endsInText)
            bindField(layoutUsed, tvUsedValue, item.usedText)

            layoutMainFields.isVisible = layoutDiscount.isVisible || layoutExpiry.isVisible
            dividerPromo.isVisible = layoutMainFields.isVisible
            layoutSecondaryFields.isVisible =
                layoutMinSpend.isVisible || layoutEndsIn.isVisible || layoutUsed.isVisible

            val isExpired = item.status == AdminPromoStatus.EXPIRED
            layoutActions.isVisible = !isExpired
            tvReactivate.isVisible = isExpired

            tvCode.paintFlags = if (isExpired) {
                tvCode.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvCode.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            btnEdit.setOnClickListener { onEditClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
            btnShare.setOnClickListener { onShareClick(item) }
            tvReactivate.setOnClickListener { onReactivateClick(item) }
        }

        private fun bindStatus(item: AdminPromoUiModel) = with(binding.tvStatus) {
            text = when (item.status) {
                AdminPromoStatus.ACTIVE -> context.getString(R.string.active)
                AdminPromoStatus.INACTIVE -> context.getString(R.string.inactive)
                AdminPromoStatus.SCHEDULED -> context.getString(R.string.scheduled)
                AdminPromoStatus.EXPIRED -> context.getString(R.string.expired)
            }

            setBackgroundResource(
                when (item.status) {
                    AdminPromoStatus.ACTIVE -> R.drawable.bg_promo_status_active
                    AdminPromoStatus.INACTIVE,
                    AdminPromoStatus.EXPIRED,
                        -> R.drawable.bg_promo_status_expired
                    AdminPromoStatus.SCHEDULED -> R.drawable.bg_promo_status_scheduled
                },
            )
        }

        private fun bindField(container: View, valueView: TextView, value: String?) {
            container.isVisible = !value.isNullOrBlank()
            valueView.text = value.orEmpty()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AdminPromoUiModel>() {
        override fun areItemsTheSame(
            oldItem: AdminPromoUiModel,
            newItem: AdminPromoUiModel,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AdminPromoUiModel,
            newItem: AdminPromoUiModel,
        ): Boolean = oldItem == newItem
    }
}