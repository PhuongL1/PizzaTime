package com.devpro.pizzatime.feature.admin.promo

enum class AdminPromoStatus {
    ACTIVE,
    INACTIVE,
    SCHEDULED,
    EXPIRED,
}

data class AdminPromoUiModel(
    val id: String,
    val code: String,
    val title: String,
    val description: String = "",
    val discountType: String = "PERCENT",
    val discountValue: Double = 0.0,
    val minOrderAmount: Double = 0.0,
    val status: AdminPromoStatus,
    val discountText: String? = null,
    val expiryText: String? = null,
    val minSpendText: String? = null,
    val endsInText: String? = null,
    val usedText: String? = null,
    val isHighlighted: Boolean = false,
)
