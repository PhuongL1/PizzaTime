import { HttpError } from "../util/httpError";
import type { OrderRecord, TrustedPricingSnapshot } from "../model/order";

const MAX_PROVIDER_AMOUNT_VND = 9_999_999_999;

export type TrustedAmount = {
  amountVnd: number;
  snapshot: TrustedPricingSnapshot;
};

export class TrustedOrderAmountService {
  resolve(order: OrderRecord): TrustedAmount {
    const snapshot = order.pricingSnapshotVnd;
    if (snapshot === undefined) {
      throw new HttpError(
        409,
        "ORDER_PRICING_INVALID",
        "Trusted integer-VND pricing snapshot is not available for this order."
      );
    }
    if (snapshot.totalVnd !== snapshot.itemsSubtotalVnd - snapshot.discountVnd + snapshot.deliveryFeeVnd) {
      throw new HttpError(409, "ORDER_AMOUNT_MISMATCH", "Trusted pricing snapshot does not reconcile.");
    }
    if (snapshot.totalVnd <= 0) {
      throw new HttpError(409, "ORDER_PRICING_INVALID", "Trusted amount must be positive.");
    }
    if (snapshot.totalVnd > MAX_PROVIDER_AMOUNT_VND) {
      throw new HttpError(409, "ORDER_PRICING_INVALID", "Trusted amount exceeds provider limits.");
    }
    if (order.finalTotal !== undefined && !Number.isInteger(order.finalTotal)) {
      throw new HttpError(
        409,
        "ORDER_PRICING_INVALID",
        "Existing order total is not stored as an integer-VND value."
      );
    }
    if (order.total !== undefined && !Number.isInteger(order.total)) {
      throw new HttpError(
        409,
        "ORDER_PRICING_INVALID",
        "Existing order total is not stored as an integer-VND value."
      );
    }
    if (order.finalTotal !== undefined && order.finalTotal !== snapshot.totalVnd) {
      throw new HttpError(409, "ORDER_AMOUNT_MISMATCH", "Stored order total does not match the trusted amount.");
    }
    if (order.total !== undefined && order.total !== snapshot.totalVnd) {
      throw new HttpError(409, "ORDER_AMOUNT_MISMATCH", "Stored order total does not match the trusted amount.");
    }
    return {
      amountVnd: snapshot.totalVnd,
      snapshot
    };
  }
}
