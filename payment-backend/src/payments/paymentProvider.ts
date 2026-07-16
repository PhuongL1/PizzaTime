import type { PaymentAttemptRecord } from "../model/paymentAttempt";
import type { PaymentProviderCode } from "../model/paymentProvider";

export type CreatedPaymentSession = {
  paymentReference: string;
  paymentPageUrl: string;
  qrPayload: string;
  providerAmount: number;
  paymentTokenHash: string;
  paymentTokenSalt: string;
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
    amountVnd: number;
  }): CreatedPaymentSession;
  rebuildSession(attempt: PaymentAttemptRecord): RebuiltPaymentSession;
}
