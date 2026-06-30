package com.devpro.pizzatime.feature.admin.staff

import com.devpro.pizzatime.R

object FakeAdminStaffData {

    val staff: List<AdminStaffUiModel> = listOf(
        AdminStaffUiModel(
            id = "staff_marco_rossi",
            name = "Marco Rossi",
            role = AdminStaffRole.KITCHEN,
            status = AdminStaffStatus.ACTIVE,
            note = "Joined Mar 2023",
            avatarRes = R.drawable.ic_admin_view_reports,
            isHighlighted = true,
        ),
        AdminStaffUiModel(
            id = "staff_elena_vance",
            name = "Elena Vance",
            role = AdminStaffRole.SHIPPER,
            status = AdminStaffStatus.INACTIVE,
            note = "Last shift: 2 days ago",
            avatarRes = R.drawable.ic_admin_view_reports,
        ),
        AdminStaffUiModel(
            id = "staff_julian_thorne",
            name = "Julian Thorne",
            role = AdminStaffRole.ADMIN,
            status = AdminStaffStatus.ACTIVE,
            note = "Night Supervisor",
            avatarRes = R.drawable.ic_admin_view_reports,
            isHighlighted = true,
        ),
        AdminStaffUiModel(
            id = "staff_sofia_chen",
            name = "Sofia Chen",
            role = AdminStaffRole.STAFF,
            status = AdminStaffStatus.ACTIVE,
            note = "On Duty: Floor",
            avatarRes = R.drawable.ic_admin_view_reports,
            isHighlighted = true,
        ),
    )
}