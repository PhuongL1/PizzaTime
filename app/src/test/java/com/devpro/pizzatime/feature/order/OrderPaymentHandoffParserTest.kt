package com.devpro.pizzatime.feature.order

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderPaymentHandoffParserTest {

    @Test
    fun `missing payment method maps to COD`() {
        assertEquals(PaymentMethod.COD, OrderPaymentHandoffParser.parsePaymentMethod(null))
    }

    @Test
    fun `missing COD status maps to NOT_REQUIRED`() {
        assertEquals(
            PaymentStatus.NOT_REQUIRED,
            OrderPaymentHandoffParser.parsePaymentStatus(PaymentMethod.COD, null),
        )
    }

    @Test
    fun `DEMO with missing status maps conservatively to PENDING`() {
        assertEquals(
            PaymentStatus.PENDING,
            OrderPaymentHandoffParser.parsePaymentStatus(PaymentMethod.DEMO, null),
        )
    }

    @Test
    fun `unknown values do not crash`() {
        val snapshot = OrderPaymentHandoffParser.parse(
            orderStatus = "PENDING",
            paymentMethodValue = "mystery",
            paymentStatusValue = "weird",
            handoffStatusValue = "odd",
            customerId = "customer",
            shipperId = "shipper",
        )

        assertEquals(PaymentMethod.UNKNOWN, snapshot.paymentMethod)
        assertEquals(PaymentStatus.UNKNOWN, snapshot.paymentStatus)
        assertEquals(DeliveryHandoffStatus.UNKNOWN, snapshot.deliveryHandoffStatus)
    }

    @Test
    fun `COD missing handoff maps to NOT_REQUIRED`() {
        assertEquals(
            DeliveryHandoffStatus.NOT_REQUIRED,
            OrderPaymentHandoffParser.parseDeliveryHandoffStatus(PaymentMethod.COD, null),
        )
    }

    @Test
    fun `DEMO missing handoff maps to LOCKED`() {
        assertEquals(
            DeliveryHandoffStatus.LOCKED,
            OrderPaymentHandoffParser.parseDeliveryHandoffStatus(PaymentMethod.DEMO, null),
        )
    }

    @Test
    fun `VNPAY still parses for future provider support`() {
        assertEquals(PaymentMethod.VNPAY, OrderPaymentHandoffParser.parsePaymentMethod("VNPAY"))
    }
}
