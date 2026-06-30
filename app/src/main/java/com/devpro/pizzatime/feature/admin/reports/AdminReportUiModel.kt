package com.devpro.pizzatime.feature.admin.reports

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.R

data class BestSellerUiModel(
    val id: String,
    val rank: String,
    val name: String,
    val soldText: String,
    val progress: Int,
    @get:DrawableRes val imageRes: Int = R.drawable.ic_admin_view_reports,
)