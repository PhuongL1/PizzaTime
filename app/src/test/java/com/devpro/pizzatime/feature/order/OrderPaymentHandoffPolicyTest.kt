package com.devpro.pizzatime.feature.order

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderPaymentHandoffPolicyTest {

    @Test
    fun `COD Staff confirmation remains allowed when existing rules allow`() {
        assertTrue(OrderPaymentHandoffPolicy.canStaffConfirmOrder(codPending()))
    }

    @Test
    fun `VNPAY PENDING blocks Staff and VNPAY PAID allows Staff`() {
        assertFalse(OrderPaymentHandoffPolicy.canStaffConfirmOrder(vnpayPending()))
        assertTrue(OrderPaymentHandoffPolicy.canStaffConfirmOrder(vnpayPaidPending()))
    }

    @Test
    fun `Assigned Shipper can mark arrived only in LOCKED`() {
        assertTrue(OrderPaymentHandoffPolicy.canShipperMarkArrived(vnpayDeliveringLocked(), SHIPPER_ID))
        assertFalse(
            OrderPaymentHandoffPolicy.canShipperMarkArrived(
                vnpayDeliveringLocked().copy(deliveryHandoffStatus = DeliveryHandoffStatus.AWAITING_CUSTOMER),
                SHIPPER_ID,
            ),
        )
    }

    @Test
    fun `Customer can confirm only in AWAITING_CUSTOMER and owner only`() {
        assertTrue(OrderPaymentHandoffPolicy.canCustomerConfirmReceipt(vnpayAwaitingCustomer(), CUSTOMER_ID))
        assertFalse(OrderPaymentHandoffPolicy.canCustomerConfirmReceipt(vnpayAwaitingCustomer(), "customer-b"))
        assertFalse(OrderPaymentHandoffPolicy.canCustomerConfirmReceipt(vnpayDeliveringLocked(), CUSTOMER_ID))
    }

    @Test
    fun `Shipper cannot complete in LOCKED or AWAITING_CUSTOMER and can after CUSTOMER_CONFIRMED`() {
        assertFalse(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(vnpayDeliveringLocked(), SHIPPER_ID))
        assertFalse(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(vnpayAwaitingCustomer(), SHIPPER_ID))
        assertTrue(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(vnpayCustomerConfirmed(), SHIPPER_ID))
    }

    @Test
    fun `COD does not require Customer confirmation`() {
        assertFalse(OrderPaymentHandoffPolicy.requiresCustomerReceipt(codPending()))
        assertTrue(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(codDelivering(), SHIPPER_ID))
    }

    @Test
    fun `cancelled and delivered disable every handoff action`() {
        listOf(OrderStatusValues.CANCELLED, OrderStatusValues.DELIVERED).forEach { terminal ->
            val state = vnpayCustomerConfirmed().copy(orderStatus = terminal)
            assertFalse(OrderPaymentHandoffPolicy.canStaffConfirmOrder(state))
            assertFalse(OrderPaymentHandoffPolicy.canShipperMarkArrived(state, SHIPPER_ID))
            assertFalse(OrderPaymentHandoffPolicy.canCustomerConfirmReceipt(state, CUSTOMER_ID))
            assertFalse(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(state, SHIPPER_ID))
        }
    }

    @Test
    fun `customer and shipper visibility policy remains scoped to eligible actor`() {
        assertFalse(OrderPaymentHandoffPolicy.shouldShowCustomerReceiptAction(codDelivering(), CUSTOMER_ID))
        assertTrue(OrderPaymentHandoffPolicy.shouldShowCustomerReceiptAction(vnpayDeliveringLocked(), CUSTOMER_ID))
        assertTrue(OrderPaymentHandoffPolicy.shouldShowCustomerReceiptAction(vnpayCustomerConfirmed().copy(orderStatus = OrderStatusValues.DELIVERED), CUSTOMER_ID))
        assertTrue(OrderPaymentHandoffPolicy.shouldShowShipperArrivalAction(vnpayDeliveringLocked(), SHIPPER_ID))
        assertTrue(OrderPaymentHandoffPolicy.shouldShowShipperCompleteAction(vnpayCustomerConfirmed(), SHIPPER_ID))
        assertFalse(OrderPaymentHandoffPolicy.shouldShowShipperCompleteAction(vnpayCustomerConfirmed(), "shipper-b"))
    }

    private fun codPending() = OrderPaymentHandoffSnapshot(
        orderStatus = OrderStatusValues.PENDING,
        paymentMethod = PaymentMethod.COD,
        paymentStatus = PaymentStatus.NOT_REQUIRED,
        deliveryHandoffStatus = DeliveryHandoffStatus.NOT_REQUIRED,
        customerId = CUSTOMER_ID,
        shipperId = SHIPPER_ID,
    )

    private fun codDelivering() = codPending().copy(orderStatus = OrderStatusValues.DELIVERING)

    private fun vnpayPending() = codPending().copy(
        paymentMethod = PaymentMethod.VNPAY,
        paymentStatus = PaymentStatus.PENDING,
        deliveryHandoffStatus = DeliveryHandoffStatus.LOCKED,
    )

    private fun vnpayPaidPending() = vnpayPending().copy(paymentStatus = PaymentStatus.PAID)

    private fun vnpayDeliveringLocked() = vnpayPaidPending().copy(orderStatus = OrderStatusValues.DELIVERING)

    private fun vnpayAwaitingCustomer() = vnpayDeliveringLocked().copy(
        deliveryHandoffStatus = DeliveryHandoffStatus.AWAITING_CUSTOMER,
    )

    private fun vnpayCustomerConfirmed() = vnpayDeliveringLocked().copy(
        deliveryHandoffStatus = DeliveryHandoffStatus.CUSTOMER_CONFIRMED,
    )

    private companion object {
        const val CUSTOMER_ID = "customer-a"
        const val SHIPPER_ID = "shipper-a"
    }
}
