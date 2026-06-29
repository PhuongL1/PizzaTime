package com.devpro.pizzatime.feature.staff.detail

import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderStatus

object FakeStaffOrderDetailData {

    private val orders = mutableListOf(
        StaffOrderDetailUiModel(
            orderId = "#ORD-8824",
            receivedAgo = "4m ago",
            status = StaffOrderStatus.PENDING,
            itemName = "TRUFFLE NOIR",
            size = "Large (16\")",
            crust = "Artisan Thin",
            toppings = listOf(
                "Double Buffalo Mozzarella",
                "Black Truffle Shavings",
                "Wild Porcini Mushrooms",
                "Roasted Garlic Confit",
                "Fresh Chives",
            ),
            imageRes = R.drawable.img_welcome_hero,
            allergyTitle = "ALLERGY: GLUTEN",
            allergyMessage = "Use separate preparation station and clean peel. Double check flour contamination.",
            customerRequest = "Extra truffle oil on the side and please char the crust slightly more than usual. It\'s for an anniversary dinner!",
            tags = listOf("VIP TIER", "ANNIVERSARY"),
        ),
    )

    fun getByOrderId(orderId: String): StaffOrderDetailUiModel {
        return orders.firstOrNull { it.orderId == orderId } ?: orders.first()
    }

    fun updateStatus(orderId: String, newStatus: StaffOrderStatus) {
        val index = orders.indexOfFirst { it.orderId == orderId }
        if (index == -1) return
        orders[index] = orders[index].copy(status = newStatus)
    }
}

