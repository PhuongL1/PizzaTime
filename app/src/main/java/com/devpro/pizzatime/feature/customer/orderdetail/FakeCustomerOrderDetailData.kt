package com.devpro.pizzatime.feature.customer.orderdetail

import com.devpro.pizzatime.R

object FakeCustomerOrderDetailData {

    fun getOrderDetail(orderId: String = "PT-9821"): CustomerOrderDetailUiModel {
        return CustomerOrderDetailUiModel(
            orderId = orderId,
            statusLabel = "DELIVERED",
            orderTime = "Oct 12, 11:24 PM",
            heroImageRes = R.drawable.img_pizza_time,
            heroImageUrl = "",
            heroMessage = "Arrived safely",
            items = listOf(
                CustomerOrderItemUiModel(
                    productId = "truffle_forest_pizza",
                    quantity = 1,
                    name = "Truffle Forest Pizza",
                    description = "Large · Thin Crust · Extra Truffle Oil",
                    price = 28.0,
                    imageRes = R.drawable.img_pizza_time,
                    imageUrl = "",
                ),
                CustomerOrderItemUiModel(
                    productId = "midnight_burrata",
                    quantity = 1,
                    name = "Midnight Burrata",
                    description = "Standard Appertivo",
                    price = 16.0,
                    imageRes = null,
                    imageUrl = "",
                ),
                CustomerOrderItemUiModel(
                    productId = "blood_orange_soda",
                    quantity = 2,
                    name = "Blood Orange Soda",
                    description = "Artisan Sicilian Soda",
                    price = 12.0,
                    imageRes = null,
                    imageUrl = "",
                ),
            ),
            bill = CustomerBillUiModel(
                subtotal = 56.0,
                deliveryFee = 4.5,
                taxes = 5.12,
                discountLabel = "Late Night Discount",
                discount = -5.0,
                total = 60.62,
            ),
            deliveryAddressTitle = "DELIVERED TO",
            deliveryAddressLine1 = "Artisan Lofts, Unit 4B",
            deliveryAddressLine2 = "128 Mercer St, New York, NY",
            distanceKm = 3.2,
            paymentMethod = "Cash on Delivery",
            paymentStatus = "Paid",
        )
    }
}
