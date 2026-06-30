package com.devpro.pizzatime.feature.shipper.dashboard

object FakeShipperDeliveryData {

    fun getActiveDelivery(): ShipperDeliveryUiModel {
        return ShipperDeliveryUiModel(
            orderId = "#PX-9921",
            customerName = "Julianne Moore",
            address = "722 Nightshade Ave, Penthouse B\nUpper East Side, NY",
            etaLabel = "ETA: 8 MIN",
            paymentLabel = "COLLECT CASH",
            paymentAmount = "$42.50",
            status = ShipperDeliveryStatus.ACTIVE,
        )
    }

    fun getAssignedDeliveries(): List<ShipperDeliveryUiModel> {
        return listOf(
            ShipperDeliveryUiModel(
                orderId = "#PX-9925",
                customerName = "Leo Sterling",
                address = "404 Echo Park Ln, Apt 4",
                etaLabel = "",
                paymentLabel = "PREPAID",
                paymentAmount = "",
                status = ShipperDeliveryStatus.ASSIGNED,
            ),
            ShipperDeliveryUiModel(
                orderId = "#PX-9928",
                customerName = "Sarah Connor",
                address = "1200 SkyNet Blvd, Suite 101",
                etaLabel = "",
                paymentLabel = "CASH",
                paymentAmount = "$18.25",
                status = ShipperDeliveryStatus.ASSIGNED,
            ),
        )
    }
}