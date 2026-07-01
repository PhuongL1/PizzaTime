package com.devpro.pizzatime.feature.customer.memberqr

import com.devpro.pizzatime.R

object FakeCustomerMemberQrData {

    fun getMemberQr(): CustomerMemberQrUiModel {
        return CustomerMemberQrUiModel(
            tierLabel = "PREMIUM TIER",
            memberTitle = "Artisan Member",
            pointsLabel = "DOUGH POINTS",
            currentPoints = 1250,
            targetPoints = 2000,
            memberSinceLabel = "MEMBER SINCE",
            memberSinceValue = "Oct 2023",
            qrInstruction = "Scan at the counter or kiosk to\nearn points and redeem\nrewards",
            qrImageRes = R.drawable.img_pizza_time,
        )
    }
}