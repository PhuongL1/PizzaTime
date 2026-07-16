import { randomBytes } from "node:crypto";

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

  createSession(input: { attemptId: string; amountVnd: number }): CreatedPaymentSession {
    const paymentTokenSalt = randomBytes(32).toString("base64url");
    const paymentToken = this.buildToken(input.attemptId, paymentTokenSalt);
    const paymentPageUrl = this.buildPaymentPageUrl(paymentToken);

    return {
      paymentReference: input.attemptId,
      paymentPageUrl,
      qrPayload: paymentPageUrl,
      providerAmount: input.amountVnd,
      paymentTokenHash: sha256Hex(paymentToken),
      paymentTokenSalt
    };
  }

  rebuildSession(attempt: PaymentAttemptRecord): RebuiltPaymentSession {
    const paymentToken = this.buildToken(attempt.id, attempt.paymentTokenSalt);
    const paymentPageUrl = this.buildPaymentPageUrl(paymentToken);
    return {
      paymentReference: attempt.transactionRef,
      paymentPageUrl,
      qrPayload: paymentPageUrl
    };
  }

  private buildToken(attemptId: string, paymentTokenSalt: string): string {
    return hmacSha256Base64Url(
      this.env.demoPaymentTokenSecret,
      `${attemptId}.${paymentTokenSalt}`
    );
  }

  private buildPaymentPageUrl(paymentToken: string): string {
    return `${this.env.publicBaseUrl}/demo/pay/${paymentToken}`;
  }
}
