import type { AppEnv } from "../config/env";
import type { PaymentAttemptRecord } from "../model/paymentAttempt";
import { hmacSha256Base64Url, sha256Hex } from "../util/hashing";

import type {
  CreatedPaymentSession,
  PaymentProvider,
  RebuiltPaymentSession
} from "./paymentProvider";

export class DemoPaymentProvider implements PaymentProvider {
  readonly code = "DEMO" as const;
  readonly paymentMethod = "DEMO" as const;

  constructor(private readonly env: Pick<AppEnv, "publicBaseUrl" | "demoPaymentTokenSecret">) {}

  createSession(input: {
    attemptId: string;
    customerId: string;
    orderId: string;
    amountVnd: number;
    expiresAt: Date;
  }): CreatedPaymentSession {
    const paymentToken = this.buildToken({
      tokenVersion: 1,
      attemptId: input.attemptId,
      customerId: input.customerId,
      orderId: input.orderId,
      expiresAt: input.expiresAt
    });
    const paymentPageUrl = this.buildPaymentPageUrl(paymentToken);

    return {
      paymentReference: input.attemptId,
      paymentPageUrl,
      qrPayload: paymentPageUrl,
      providerAmount: input.amountVnd,
      paymentTokenHash: sha256Hex(paymentToken),
      paymentTokenVersion: 1
    };
  }

  rebuildSession(attempt: PaymentAttemptRecord): RebuiltPaymentSession {
    const paymentToken = this.buildToken({
      tokenVersion: attempt.paymentTokenVersion,
      attemptId: attempt.id,
      customerId: attempt.customerId,
      orderId: attempt.orderId,
      expiresAt: attempt.expiresAt.toDate()
    });
    const paymentPageUrl = this.buildPaymentPageUrl(paymentToken);
    return {
      paymentReference: attempt.transactionRef,
      paymentPageUrl,
      qrPayload: paymentPageUrl
    };
  }

  private buildToken(input: {
    tokenVersion: 1;
    attemptId: string;
    customerId: string;
    orderId: string;
    expiresAt: Date;
  }): string {
    return hmacSha256Base64Url(
      this.env.demoPaymentTokenSecret,
      [
        `v${input.tokenVersion}`,
        input.customerId,
        input.orderId,
        input.attemptId,
        input.expiresAt.toISOString()
      ].join("|")
    );
  }

  private buildPaymentPageUrl(paymentToken: string): string {
    return `${this.env.publicBaseUrl}/demo/pay/${paymentToken}`;
  }
}
