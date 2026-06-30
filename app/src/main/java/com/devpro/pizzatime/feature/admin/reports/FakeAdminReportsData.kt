package com.devpro.pizzatime.feature.admin.reports

import com.devpro.pizzatime.R

object FakeAdminReportsData {

    val bestSellers: List<BestSellerUiModel> = listOf(
        BestSellerUiModel(
            id = "truffle_honey_artisan",
            rank = "#1",
            name = "Truffle Honey Artisan",
            soldText = "48 SOLD",
            progress = 86,
            imageRes = R.drawable.ic_admin_view_reports,
        ),
        BestSellerUiModel(
            id = "spicy_calabrese",
            rank = "#2",
            name = "Spicy Calabrese",
            soldText = "34 SOLD",
            progress = 62,
            imageRes = R.drawable.ic_admin_view_reports,
        ),
        BestSellerUiModel(
            id = "forest_mushroom",
            rank = "#3",
            name = "Forest Mushroom",
            soldText = "29 SOLD",
            progress = 52,
            imageRes = R.drawable.ic_admin_view_reports,
        ),
    )
}