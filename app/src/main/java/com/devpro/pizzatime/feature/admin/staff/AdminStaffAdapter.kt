package com.devpro.pizzatime.feature.admin.staff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.ItemAdminStaffBinding

class AdminStaffAdapter(
    private val onEditClick: (AdminStaffUiModel) -> Unit,
    private val onToggleStatusClick: (AdminStaffUiModel) -> Unit,
) : ListAdapter<AdminStaffUiModel, AdminStaffAdapter.AdminStaffViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminStaffViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAdminStaffBinding.inflate(inflater, parent, false)
        return AdminStaffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminStaffViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminStaffViewHolder(
        private val binding: ItemAdminStaffBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AdminStaffUiModel) = with(binding) {
            rootStaffCard.setBackgroundResource(
                if (item.isHighlighted) {
                    R.drawable.bg_admin_staff_card_highlight
                } else {
                    R.drawable.bg_admin_staff_card
                },
            )

            ivAvatar.setImageResource(item.avatarRes)
            tvStaffName.text = item.name
            tvStaffRole.text = item.role.toDisplayText()
            tvStaffNote.text = item.note
            tvStatus.text = item.status.toDisplayText()

            tvStatus.setBackgroundResource(
                when (item.status) {
                    AdminStaffStatus.ACTIVE -> R.drawable.bg_admin_staff_status_active
                    AdminStaffStatus.INACTIVE -> R.drawable.bg_admin_staff_status_inactive
                },
            )

            btnEditStaff.setOnClickListener { onEditClick(item) }
            btnToggleStaffStatus.setOnClickListener { onToggleStatusClick(item) }
        }

        private fun AdminStaffRole.toDisplayText(): String {
            return when (this) {
                AdminStaffRole.KITCHEN -> "KITCHEN"
                AdminStaffRole.SHIPPER -> "SHIPPER"
                AdminStaffRole.ADMIN -> "ADMIN"
                AdminStaffRole.STAFF -> "STAFF"
                AdminStaffRole.CUSTOMER -> "CUSTOMER"
            }
        }

        private fun AdminStaffStatus.toDisplayText(): String {
            return when (this) {
                AdminStaffStatus.ACTIVE -> "ACTIVE"
                AdminStaffStatus.INACTIVE -> "INACTIVE"
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AdminStaffUiModel>() {
        override fun areItemsTheSame(
            oldItem: AdminStaffUiModel,
            newItem: AdminStaffUiModel,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AdminStaffUiModel,
            newItem: AdminStaffUiModel,
        ): Boolean = oldItem == newItem
    }
}
