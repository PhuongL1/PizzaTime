export const PAYMENT_PROVIDERS = ["DEMO", "VNPAY"] as const;

export type PaymentProviderCode = (typeof PAYMENT_PROVIDERS)[number];

export function isPrepaidPaymentMethod(value: string): boolean {
  return value === "DEMO" || value === "VNPAY";
}
