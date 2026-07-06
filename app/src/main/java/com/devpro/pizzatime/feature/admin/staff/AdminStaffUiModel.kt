package com.devpro.pizzatime.feature.admin.staff

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.R

enum class AdminStaffRole {
    KITCHEN,
    SHIPPER,
    ADMIN,
    STAFF,
    CUSTOMER,
}

enum class AdminStaffStatus {
    ACTIVE,
    INACTIVE,
}

data class AdminStaffUiModel(
    val id: String,
    val name: String,
    val role: AdminStaffRole,
    val status: AdminStaffStatus,
    val note: String,
    @get:DrawableRes val avatarRes: Int = R.drawable.ic_admin_view_reports,
    val isHighlighted: Boolean = false,
)
