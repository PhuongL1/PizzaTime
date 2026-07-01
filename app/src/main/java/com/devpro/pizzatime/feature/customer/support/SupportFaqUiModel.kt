package com.devpro.pizzatime.feature.customer.support

import androidx.annotation.StringRes

enum class SupportTopicCategory {
    ALL,
    DELIVERY,
    PAYMENTS,
}

data class SupportFaqUiModel(
    val id: String,
    @param:StringRes val questionRes: Int,
    @param:StringRes val answerRes: Int,
    val category: SupportTopicCategory,
    val isExpanded: Boolean = false,
)