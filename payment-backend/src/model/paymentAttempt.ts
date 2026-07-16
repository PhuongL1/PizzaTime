import { Timestamp } from "firebase-admin/firestore";
import { z } from "zod";

import type { PaymentProviderCode } from "./paymentProvider";
import type { PaymentAttemptStatus } from "./paymentStatus";

export const paymentAttemptDocumentSchema = z
  .object({
    schemaVersion: z.literal(1),
    provider: z.enum(["DEMO", "VNPAY"]),
    status: z.enum(["PENDING", "PAID", "FAILED", "EXPIRED", "REVIEW_REQUIRED"]),
    orderId: z.string().min(1),
    customerId: z.string().min(1),
    transactionRef: z.string().min(1),
    requestIdHash: z.string().min(1),
    amountVnd: z.int().positive(),
    providerAmount: z.int().positive(),
    currency: z.literal("VND"),
    paymentTokenHash: z.string().length(64),
    paymentTokenVersion: z.literal(1),
    createdAt: z.instanceof(Timestamp),
    expiresAt: z.instanceof(Timestamp),
    updatedAt: z.instanceof(Timestamp),
    confirmedAt: z.instanceof(Timestamp).optional(),
    providerTransactionId: z.string().optional(),
    failureCode: z.string().optional(),
    tokenConsumedAt: z.instanceof(Timestamp).optional()
  })
  .strict();

export type PaymentAttemptRecord = {
  id: string;
  schemaVersion: 1;
  provider: PaymentProviderCode;
  status: PaymentAttemptStatus;
  orderId: string;
  customerId: string;
  transactionRef: string;
  requestIdHash: string;
  amountVnd: number;
  providerAmount: number;
  currency: "VND";
  paymentTokenHash: string;
  paymentTokenVersion: 1;
  createdAt: Timestamp;
  expiresAt: Timestamp;
  updatedAt: Timestamp;
  confirmedAt?: Timestamp;
  providerTransactionId?: string;
  failureCode?: string;
  tokenConsumedAt?: Timestamp;
};
