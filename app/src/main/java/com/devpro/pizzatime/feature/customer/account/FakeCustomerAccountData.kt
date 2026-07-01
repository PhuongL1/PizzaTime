package com.devpro.pizzatime.feature.customer.account

import com.devpro.pizzatime.R

object FakeCustomerAccountData {

    fun getCustomerAccount(): CustomerAccountUiModel {
        return CustomerAccountUiModel(
            fullName = "Julian Vane",
            tierName = "ARTISAN MEMBER",
            doughPoints = 1250,
            email = "j.vane@artisan.mail",
            phone = "+1 (555) 012-3456",
            avatarRes = R.drawable.ic_customer_account_avatar_placeholder,
        )
    }
}