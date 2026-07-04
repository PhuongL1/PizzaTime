package com.devpro.pizzatime.feature.admin.menu

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.databinding.ItemAdminMenuBinding

class AdminMenuAdapter(
    private val onAvailabilityClick: (AdminMenuUiModel) -> Unit,
    private val onEditClick: (AdminMenuUiModel) -> Unit,
) : ListAdapter<AdminMenuUiModel, AdminMenuAdapter.AdminMenuViewHolder>(
    AdminMenuDiffCallback,
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminMenuViewHolder {
        val binding = ItemAdminMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AdminMenuViewHolder(
            binding = binding,
            onAvailabilityClick = onAvailabilityClick,
            onEditClick = onEditClick,
        )
    }

    override fun onBindViewHolder(holder: AdminMenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AdminMenuViewHolder(
        private val binding: ItemAdminMenuBinding,
        private val onAvailabilityClick: (AdminMenuUiModel) -> Unit,
        private val onEditClick: (AdminMenuUiModel) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AdminMenuUiModel) = with(binding) {
            val context = root.context

            ivMenuImage.loadProductImage(
                imageUrl = item.imageUrl,
                fallbackRes = item.imageRes,
            )
            tvCategoryBadge.text = context.getString(item.category.labelRes)
            tvMenuName.text = item.name
            tvMenuDescription.text = item.description
            tvMenuPrice.text = item.price

            tvSoldOut.visibility = if (item.isAvailable) View.GONE else View.VISIBLE
            toggleAvailability.setBackgroundResource(
                if (item.isAvailable) {
                    R.drawable.bg_manage_menu_toggle_on
                } else {
                    R.drawable.bg_manage_menu_toggle_off
                },
            )
            toggleAvailability.gravity = if (item.isAvailable) Gravity.END else Gravity.START

            toggleAvailability.setOnClickListener {
                onAvailabilityClick(item)
            }

            btnEdit.setOnClickListener {
                onEditClick(item)
            }
        }
    }
}

private object AdminMenuDiffCallback : DiffUtil.ItemCallback<AdminMenuUiModel>() {

    override fun areItemsTheSame(
        oldItem: AdminMenuUiModel,
        newItem: AdminMenuUiModel,
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: AdminMenuUiModel,
        newItem: AdminMenuUiModel,
    ): Boolean = oldItem == newItem
}
