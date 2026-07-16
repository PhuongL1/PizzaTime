import { z } from "zod";

import type { OrderPaymentStatus } from "./paymentStatus";

export const trustedPricingSnapshotSchema = z
  .object({
    schemaVersion: z.literal(1),
    currency: z.literal("VND"),
    itemsSubtotalVnd: z.int().nonnegative(),
    discountVnd: z.int().nonnegative(),
    deliveryFeeVnd: z.int().nonnegative(),
    totalVnd: z.int().positive()
  })
  .strict();

export type TrustedPricingSnapshot = z.infer<typeof trustedPricingSnapshotSchema>;

export type OrderRecord = {
  id: string;
  orderCode?: string;
  customerId: string;
  status: string;
  paymentMethod: string;
  paymentStatus: OrderPaymentStatus;
  paymentProvider?: string;
  paymentAttemptId?: string;
  paymentReference?: string;
  providerTransactionId?: string;
  finalTotal?: number;
  total?: number;
  pricingSnapshotVnd?: TrustedPricingSnapshot;
  raw: Record<string, unknown>;
};
