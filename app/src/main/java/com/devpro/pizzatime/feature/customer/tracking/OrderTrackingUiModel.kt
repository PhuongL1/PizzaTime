package com.devpro.pizzatime.feature.customer.tracking

import androidx.annotation.DrawableRes
import com.devpro.pizzatime.R

enum class TrackingStepState {
    DONE,
    CURRENT,
    PENDING,
}

data class TrackingStepUiModel(
    val title: String,
    val subtitle: String,
    val state: TrackingStepState,
)

data class TrackingProductUiModel(
    val name: String,
    val optionText: String,
    val price: String,
    @param:DrawableRes val imageRes: Int,
)

object FakeTrackingData {

    val steps = listOf(
        TrackingStepUiModel(
            title = "Order Placed",
            subtitle = "10:45 PM · Successfully received",
            state = TrackingStepState.DONE,
        ),
        TrackingStepUiModel(
            title = "Preparing",
            subtitle = "10:50 PM · Artisans at work",
            state = TrackingStepState.DONE,
        ),
        TrackingStepUiModel(
            title = "Baking Now",
            subtitle = "In our wood-fired stone oven",
            state = TrackingStepState.CURRENT,
        ),
        TrackingStepUiModel(
            title = "Out for Delivery",
            subtitle = "Estimated 11:15 PM",
            state = TrackingStepState.PENDING,
        ),
        TrackingStepUiModel(
            title = "Delivered",
            subtitle = "",
            state = TrackingStepState.PENDING,
        ),
    )

    val product = TrackingProductUiModel(
        name = "The Midnight Truffle",
        optionText = "x1 Large · Extra Basil ·",
        price = "\$32.00",
        imageRes = R.drawable.img_welcome_hero,
    )
}