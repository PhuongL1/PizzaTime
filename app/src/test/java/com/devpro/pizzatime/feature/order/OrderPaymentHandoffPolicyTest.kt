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
    fun `prepaid PENDING blocks Staff and prepaid PAID allows Staff`() {
        assertFalse(OrderPaymentHandoffPolicy.canStaffConfirmOrder(prepaidPending()))
        assertTrue(OrderPaymentHandoffPolicy.canStaffConfirmOrder(prepaidPaidPending()))
    }

    @Test
    fun `Assigned Shipper can mark arrived only in LOCKED`() {
        assertTrue(OrderPaymentHandoffPolicy.canShipperMarkArrived(prepaidDeliveringLocked(), SHIPPER_ID))
        assertFalse(
            OrderPaymentHandoffPolicy.canShipperMarkArrived(
                prepaidDeliveringLocked().copy(deliveryHandoffStatus = DeliveryHandoffStatus.AWAITING_CUSTOMER),
                SHIPPER_ID,
            ),
        )
    }

    @Test
    fun `Customer can confirm only in AWAITING_CUSTOMER and owner only`() {
        assertTrue(OrderPaymentHandoffPolicy.canCustomerConfirmReceipt(prepaidAwaitingCustomer(), CUSTOMER_ID))
        assertFalse(OrderPaymentHandoffPolicy.canCustomerConfirmReceipt(prepaidAwaitingCustomer(), "customer-b"))
        assertFalse(OrderPaymentHandoffPolicy.canCustomerConfirmReceipt(prepaidDeliveringLocked(), CUSTOMER_ID))
    }

    @Test
    fun `Shipper cannot complete in LOCKED or AWAITING_CUSTOMER and can after CUSTOMER_CONFIRMED`() {
        assertFalse(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(prepaidDeliveringLocked(), SHIPPER_ID))
        assertFalse(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(prepaidAwaitingCustomer(), SHIPPER_ID))
        assertTrue(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(prepaidCustomerConfirmed(), SHIPPER_ID))
    }

    @Test
    fun `COD does not require Customer confirmation`() {
        assertFalse(OrderPaymentHandoffPolicy.requiresCustomerReceipt(codPending()))
        assertTrue(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(codDelivering(), SHIPPER_ID))
    }

    @Test
    fun `cancelled and delivered disable every handoff action`() {
        listOf(OrderStatusValues.CANCELLED, OrderStatusValues.DELIVERED).forEach { terminal ->
            val state = prepaidCustomerConfirmed().copy(orderStatus = terminal)
            assertFalse(OrderPaymentHandoffPolicy.canStaffConfirmOrder(state))
            assertFalse(OrderPaymentHandoffPolicy.canShipperMarkArrived(state, SHIPPER_ID))
            assertFalse(OrderPaymentHandoffPolicy.canCustomerConfirmReceipt(state, CUSTOMER_ID))
            assertFalse(OrderPaymentHandoffPolicy.canShipperCompleteDelivery(state, SHIPPER_ID))
        }
    }

    @Test
    fun `customer and shipper visibility policy remains scoped to eligible actor`() {
        assertFalse(OrderPaymentHandoffPolicy.shouldShowCustomerReceiptAction(codDelivering(), CUSTOMER_ID))
        assertTrue(OrderPaymentHandoffPolicy.shouldShowCustomerReceiptAction(prepaidDeliveringLocked(), CUSTOMER_ID))
        assertTrue(OrderPaymentHandoffPolicy.shouldShowCustomerReceiptAction(prepaidCustomerConfirmed().copy(orderStatus = OrderStatusValues.DELIVERED), CUSTOMER_ID))
        assertTrue(OrderPaymentHandoffPolicy.shouldShowShipperArrivalAction(prepaidDeliveringLocked(), SHIPPER_ID))
        assertTrue(OrderPaymentHandoffPolicy.shouldShowShipperCompleteAction(prepaidCustomerConfirmed(), SHIPPER_ID))
        assertFalse(OrderPaymentHandoffPolicy.shouldShowShipperCompleteAction(prepaidCustomerConfirmed(), "shipper-b"))
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

    private fun prepaidPending() = codPending().copy(
        paymentMethod = PaymentMethod.DEMO,
        paymentStatus = PaymentStatus.PENDING,
        deliveryHandoffStatus = DeliveryHandoffStatus.LOCKED,
    )

    private fun prepaidPaidPending() = prepaidPending().copy(paymentStatus = PaymentStatus.PAID)

    private fun prepaidDeliveringLocked() = prepaidPaidPending().copy(orderStatus = OrderStatusValues.DELIVERING)

    private fun prepaidAwaitingCustomer() = prepaidDeliveringLocked().copy(
        deliveryHandoffStatus = DeliveryHandoffStatus.AWAITING_CUSTOMER,
    )

    private fun prepaidCustomerConfirmed() = prepaidDeliveringLocked().copy(
        deliveryHandoffStatus = DeliveryHandoffStatus.CUSTOMER_CONFIRMED,
    )

    private companion object {
        const val CUSTOMER_ID = "customer-a"
        const val SHIPPER_ID = "shipper-a"
    }
}
