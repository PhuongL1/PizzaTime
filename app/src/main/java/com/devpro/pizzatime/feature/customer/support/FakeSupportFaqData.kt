package com.devpro.pizzatime.feature.customer.support

import com.devpro.pizzatime.R

object FakeSupportFaqData {

    fun getFaqs(): List<SupportFaqUiModel> {
        return listOf(
            SupportFaqUiModel(
                id = "delivery_areas",
                questionRes = R.string.support_faq_delivery_areas_question,
                answerRes = R.string.support_faq_delivery_areas_answer,
                category = SupportTopicCategory.DELIVERY,
            ),
            SupportFaqUiModel(
                id = "track_order",
                questionRes = R.string.support_faq_track_order_question,
                answerRes = R.string.support_faq_track_order_answer,
                category = SupportTopicCategory.DELIVERY,
            ),
            SupportFaqUiModel(
                id = "customize_order",
                questionRes = R.string.support_faq_customize_order_question,
                answerRes = R.string.support_faq_customize_order_answer,
                category = SupportTopicCategory.ALL,
            ),
            SupportFaqUiModel(
                id = "sanctuary_fee",
                questionRes = R.string.support_faq_sanctuary_fee_question,
                answerRes = R.string.support_faq_sanctuary_fee_answer,
                category = SupportTopicCategory.PAYMENTS,
            ),
        )
    }
}