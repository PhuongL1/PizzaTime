package com.devpro.pizzatime.feature.order

import androidx.annotation.StringRes
import com.devpro.pizzatime.R

object OrderPaymentHandoffPresentation {

    @StringRes
    fun paymentMethodLabel(method: PaymentMethod): Int {
        return when (method) {
            PaymentMethod.COD -> R.string.payment_method_cash_on_delivery
            PaymentMethod.VNPAY -> R.string.payment_method_vnpay
            PaymentMethod.UNKNOWN -> R.string.payment_method_unavailable
        }
    }

    @StringRes
    fun paymentStatusLabel(
        method: PaymentMethod,
        status: PaymentStatus,
    ): Int {
        return if (method == PaymentMethod.COD) {
            R.string.payment_status_not_required
        } else {
            when (status) {
                PaymentStatus.NOT_REQUIRED -> R.string.payment_status_not_required
                PaymentStatus.PENDING -> R.string.payment_status_pending
                PaymentStatus.PAID -> R.string.payment_status_paid
                PaymentStatus.FAILED -> R.string.payment_status_failed
                PaymentStatus.EXPIRED -> R.string.payment_status_expired
                PaymentStatus.REFUNDED -> R.string.payment_status_refunded
                PaymentStatus.UNKNOWN -> R.string.payment_status_unavailable
            }
        }
    }

    @StringRes
    fun handoffStatusLabel(status: DeliveryHandoffStatus): Int {
        return when (status) {
            DeliveryHandoffStatus.NOT_REQUIRED -> R.string.delivery_handoff_not_required
            DeliveryHandoffStatus.LOCKED -> R.string.delivery_handoff_in_progress
            DeliveryHandoffStatus.AWAITING_CUSTOMER -> R.string.delivery_handoff_waiting_for_customer
            DeliveryHandoffStatus.CUSTOMER_CONFIRMED -> R.string.delivery_handoff_customer_confirmed
            DeliveryHandoffStatus.COMPLETED -> R.string.delivery_handoff_completed
            DeliveryHandoffStatus.UNKNOWN -> R.string.delivery_handoff_unavailable
        }
    }
}
