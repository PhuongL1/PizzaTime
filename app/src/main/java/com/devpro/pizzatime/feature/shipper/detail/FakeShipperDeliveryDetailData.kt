package com.devpro.pizzatime.feature.shipper.detail

object FakeShipperDeliveryDetailData {

    private val details = listOf(
        ShipperDeliveryDetailUiModel(
            orderId = "#PX-9921",
            customerName = "Alessandra Rossi",
            address = "422 Artisan Way, Suite 8B\nManhattan, NY 10014",
            courierNote = "Gate code is 1992. Please leave at the foyer desk and ring the intercom twice. Bell is slightly loose.",
            paymentAmount = "$42.50",
            paymentMethod = "CASH ON DELIVERY",
            items = listOf(
                ShipperPaymentItemUiModel(
                    name = "1x Midnight Truffle Pizza (Large)",
                    price = "$34.00",
                ),
                ShipperPaymentItemUiModel(
                    name = "1x Rosemary Infused Wings",
                    price = "$8.50",
                ),
            ),
        ),
        ShipperDeliveryDetailUiModel(
            orderId = "#PX-9925",
            customerName = "Leo Sterling",
            address = "404 Echo Park Ln, Apt 4",
            courierNote = "Call when arriving. Customer will meet at lobby.",
            paymentAmount = "$0.00",
            paymentMethod = "PREPAID",
            items = listOf(
                ShipperPaymentItemUiModel(
                    name = "2x Midnight Pepperoni",
                    price = "$29.00",
                ),
            ),
        ),
        ShipperDeliveryDetailUiModel(
            orderId = "#PX-9928",
            customerName = "Sarah Connor",
            address = "1200 SkyNet Blvd, Suite 101",
            courierNote = "Leave with reception if unavailable.",
            paymentAmount = "$18.25",
            paymentMethod = "CASH ON DELIVERY",
            items = listOf(
                ShipperPaymentItemUiModel(
                    name = "1x Tuscan Garden",
                    price = "$18.25",
                ),
            ),
        ),
    )

    fun getDetail(orderId: String?): ShipperDeliveryDetailUiModel {
        return details.firstOrNull { it.orderId == orderId } ?: details.first()
    }
}