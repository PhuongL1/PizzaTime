package com.devpro.pizzatime.feature.admin.promo

object FakeAdminPromoData {

    val promos: List<AdminPromoUiModel> = listOf(
        AdminPromoUiModel(
            id = "promo_midnight_20",
            code = "MIDNIGHT20",
            title = "20% Off Artisanal Range",
            status = AdminPromoStatus.ACTIVE,
            discountText = "20% Total Order",
            expiryText = "Oct 31, 2024",
            isHighlighted = true,
        ),
        AdminPromoUiModel(
            id = "promo_latebird",
            code = "LATEBIRD",
            title = "Free Truffle Dip",
            status = AdminPromoStatus.ACTIVE,
            minSpendText = "$45.00",
            endsInText = "2 Days",
        ),
        AdminPromoUiModel(
            id = "promo_welcome_10",
            code = "WELCOME10",
            title = "First Order Discount",
            status = AdminPromoStatus.EXPIRED,
            usedText = "1,240 times",
        ),
    )
}