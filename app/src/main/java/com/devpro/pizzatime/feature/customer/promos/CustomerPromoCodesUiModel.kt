package com.devpro.pizzatime.feature.customer.promos

import androidx.annotation.DrawableRes

data class CustomerPromoCodesUiModel(
    val title: String,
    val subtitle: String,
    val activePromos: List<CustomerPromoUiModel>,
    val pastPromos: List<CustomerPromoUiModel>,
)

data class CustomerPromoUiModel(
    val id: String,
    val category: String,
    val code: String,
    val description: String,
    val metaLabel: String,
    val metaValue: String,
    val statusLabel: String,
    val actionLabel: String?,
    @param:DrawableRes val imageRes: Int?,
    val state: CustomerPromoState,
)

enum class CustomerPromoState {
    ACTIVE,
    USED,
    EXPIRED,
}