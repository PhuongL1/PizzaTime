import type { PaymentAttemptRecord } from "../model/paymentAttempt";
import type { PaymentProviderCode } from "../model/paymentProvider";

export type CreatedPaymentSession = {
  paymentReference: string;
  paymentPageUrl: string;
  qrPayload: string;
  providerAmount: number;
  paymentTokenHash: string;
  paymentTokenVersion: 1;
};

export type RebuiltPaymentSession = {
  paymentReference: string;
  paymentPageUrl: string;
  qrPayload: string;
};

export interface PaymentProvider {
  readonly code: PaymentProviderCode;
  readonly paymentMethod: PaymentProviderCode;
  createSession(input: {
    attemptId: string;
    customerId: string;
    orderId: string;
    amountVnd: number;
    expiresAt: Date;
  }): CreatedPaymentSession;
  rebuildSession(attempt: PaymentAttemptRecord): RebuiltPaymentSession;
}
