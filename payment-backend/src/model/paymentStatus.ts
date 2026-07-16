export const ORDER_PAYMENT_STATUSES = [
  "NOT_REQUIRED",
  "PENDING",
  "PAID",
  "FAILED",
  "EXPIRED",
  "REFUNDED",
  "UNKNOWN"
] as const;

export type OrderPaymentStatus = (typeof ORDER_PAYMENT_STATUSES)[number];

export const PAYMENT_ATTEMPT_STATUSES = [
  "PENDING",
  "PAID",
  "FAILED",
  "EXPIRED",
  "REVIEW_REQUIRED"
] as const;

export type PaymentAttemptStatus = (typeof PAYMENT_ATTEMPT_STATUSES)[number];
