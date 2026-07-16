import { describe, expect, it } from "vitest";

import type { AppEnv } from "../../src/config/env";
import { DemoPaymentProvider } from "../../src/payments/demoPaymentProvider";
import { safeCompareHex, sha256Hex } from "../../src/util/hashing";

const env: AppEnv = {
  nodeEnv: "test",
  port: 8080,
  firebaseProjectId: "demo-project",
  paymentProvider: "DEMO",
  demoPaymentEnabled: true,
  demoPaymentTokenSecret: "demo-secret-demo-secret-demo-secret-1234",
  publicBaseUrl: "https://payments.example.test",
  paymentSessionMinutes: 15
};

describe("DemoPaymentProvider", () => {
  it("creates a random token-backed payment page url and never stores the raw token", () => {
    const provider = new DemoPaymentProvider(env);

    const first = provider.createSession({
      attemptId: "PTDEMO1",
      amountVnd: 123000
    });
    const second = provider.createSession({
      attemptId: "PTDEMO2",
      amountVnd: 123000
    });

    expect(first.paymentPageUrl).toMatch(/^https:\/\/payments\.example\.test\/demo\/pay\/[A-Za-z0-9_-]+$/);
    expect(first.qrPayload).toBe(first.paymentPageUrl);
    expect(first.paymentTokenHash).toMatch(/^[a-f0-9]{64}$/);
    expect(first.paymentTokenHash).not.toContain(first.paymentTokenSalt);
    expect(first.paymentPageUrl).not.toBe(second.paymentPageUrl);
  });

  it("rebuilds the same payment page url from the stored salt and attempt id", () => {
    const provider = new DemoPaymentProvider(env);
    const created = provider.createSession({
      attemptId: "PTDEMO3",
      amountVnd: 123000
    });

    const rebuilt = provider.rebuildSession({
      id: "PTDEMO3",
      schemaVersion: 1,
      provider: "DEMO",
      status: "PENDING",
      orderId: "order-1",
      customerId: "customer-a",
      transactionRef: "PTDEMO3",
      requestIdHash: "hash",
      amountVnd: 123000,
      providerAmount: 123000,
      currency: "VND",
      paymentTokenHash: created.paymentTokenHash,
      paymentTokenSalt: created.paymentTokenSalt,
      createdAt: {} as never,
      expiresAt: {} as never,
      updatedAt: {} as never
    });

    expect(rebuilt.paymentPageUrl).toBe(created.paymentPageUrl);
    expect(rebuilt.qrPayload).toBe(created.qrPayload);
  });

  it("constant-time compare helper safely rejects unequal lengths", () => {
    expect(safeCompareHex("abcd", "abc")).toBe(false);
    expect(safeCompareHex(sha256Hex("left"), sha256Hex("right"))).toBe(false);
  });
});
