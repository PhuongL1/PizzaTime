package com.devpro.pizzatime.feature.customer.promos

import com.devpro.pizzatime.R

object FakeCustomerPromoCodesData {

    fun getPromoCodes(): CustomerPromoCodesUiModel {
        return CustomerPromoCodesUiModel(
            title = "Rewards & Promos",
            subtitle = "Elevate your late-night ritual with exclusive artisan offers.",
            activePromos = listOf(
                CustomerPromoUiModel(
                    id = "midnight20",
                    category = "LIMITED EDITION",
                    code = "MIDNIGHT20",
                    description = "20% off all signature wood-fired pizzas after 10 PM. Valid for delivery or pickup.",
                    metaLabel = "EXPIRES",
                    metaValue = "Oct 24, 2024",
                    statusLabel = "2 DAYS LEFT",
                    actionLabel = "APPLY",
                    imageRes = R.drawable.img_pizza_time,
                    state = CustomerPromoState.ACTIVE,
                ),
                CustomerPromoUiModel(
                    id = "complementary",
                    category = "LOYALTY REWARD",
                    code = "COMPLEMENTARY",
                    description = "Free appetizer or artisan beverage with any whole pizza purchase. Exclusively for Artisan Members.",
                    metaLabel = "STATUS",
                    metaValue = "One-time Use",
                    statusLabel = "ONGOING",
                    actionLabel = "APPLY",
                    imageRes = R.drawable.img_pizza_time,
                    state = CustomerPromoState.ACTIVE,
                ),
            ),
            pastPromos = listOf(
                CustomerPromoUiModel(
                    id = "start10",
                    category = "WELCOME GIFT",
                    code = "START10",
                    description = "Initial welcome discount for your first artisan experience.",
                    metaLabel = "USED",
                    metaValue = "Used on Sep 12, 2024",
                    statusLabel = "USED",
                    actionLabel = null,
                    imageRes = null,
                    state = CustomerPromoState.USED,
                ),
                CustomerPromoUiModel(
                    id = "summerend",
                    category = "SEASONAL EVENT",
                    code = "SUMMEREND",
                    description = "Farewell to the season with 15% off our Mediterranean specialty pizzas.",
                    metaLabel = "EXPIRED",
                    metaValue = "Expired Aug 31, 2024",
                    statusLabel = "EXPIRED",
                    actionLabel = null,
                    imageRes = null,
                    state = CustomerPromoState.EXPIRED,
                ),
            ),
        )
    }
}