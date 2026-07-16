import { HttpError } from "../util/httpError";
import type { OrderRecord } from "../model/order";
import type { PaymentProviderCode } from "../model/paymentProvider";

const TERMINAL_STATUSES = new Set(["CANCELLED", "DELIVERED"]);

export function assertCustomerOwnsOrder(order: OrderRecord, customerId: string): void {
  if (order.customerId !== customerId) {
    throw new HttpError(403, "ORDER_FORBIDDEN", "Order does not belong to the caller.");
  }
}

export function assertOrderPayable(order: OrderRecord, paymentMethod: PaymentProviderCode): void {
  if (order.paymentMethod !== paymentMethod) {
    throw new HttpError(
      409,
      "ORDER_NOT_PAYABLE",
      "Order is not configured for this payment provider."
    );
  }
  if (order.paymentStatus === "PAID") {
    throw new HttpError(409, "ORDER_ALREADY_PAID", "Order has already been paid.");
  }
  if (TERMINAL_STATUSES.has(order.status)) {
    throw new HttpError(409, "ORDER_NOT_PAYABLE", "Order is not in a payable state.");
  }
}
