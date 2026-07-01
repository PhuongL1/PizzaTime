package com.devpro.pizzatime.shared.dialog

import com.devpro.pizzatime.R

object FakeAssignableShipperData {

    fun getShippers(): List<AssignableShipperUiModel> {
        return listOf(
            AssignableShipperUiModel(
                id = "shipper_leo",
                name = "Leo Carter",
                activeDeliveryCount = 1,
                etaMinutes = 8,
                isAvailable = true,
                avatarRes = R.drawable.ic_customer_account_avatar_placeholder,
            ),
            AssignableShipperUiModel(
                id = "shipper_mina",
                name = "Mina Tran",
                activeDeliveryCount = 0,
                etaMinutes = 5,
                isAvailable = true,
                avatarRes = R.drawable.ic_customer_account_avatar_placeholder,
            ),
            AssignableShipperUiModel(
                id = "shipper_alex",
                name = "Alex Brown",
                activeDeliveryCount = 3,
                etaMinutes = 14,
                isAvailable = false,
                avatarRes = R.drawable.ic_customer_account_avatar_placeholder,
            ),
        )
    }
}