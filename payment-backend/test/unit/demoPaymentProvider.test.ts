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
  it("creates a deterministic secret-derived payment page url and never stores the raw token", () => {
    const provider = new DemoPaymentProvider(env);

    const first = provider.createSession({
      attemptId: "PTDEMO1",
      customerId: "customer-a",
      orderId: "order-1",
      amountVnd: 123000,
      expiresAt: new Date("2026-07-16T08:15:00.000Z")
    });
    const second = provider.createSession({
      attemptId: "PTDEMO2",
      customerId: "customer-a",
      orderId: "order-1",
      amountVnd: 123000,
      expiresAt: new Date("2026-07-16T08:15:00.000Z")
    });
    const repeated = provider.createSession({
      attemptId: "PTDEMO1",
      customerId: "customer-a",
      orderId: "order-1",
      amountVnd: 123000,
      expiresAt: new Date("2026-07-16T08:15:00.000Z")
    });

    expect(first.paymentPageUrl).toMatch(/^https:\/\/payments\.example\.test\/demo\/pay\/[A-Za-z0-9_-]+$/);
    expect(first.qrPayload).toBe(first.paymentPageUrl);
    expect(first.paymentTokenHash).toMatch(/^[a-f0-9]{64}$/);
    expect(first.paymentTokenVersion).toBe(1);
    expect(first.paymentPageUrl).not.toBe(second.paymentPageUrl);
    expect(first.paymentPageUrl).toBe(repeated.paymentPageUrl);
  });

  it("rebuilds the same payment page url from persisted attempt fields", () => {
    const provider = new DemoPaymentProvider(env);
    const created = provider.createSession({
      attemptId: "PTDEMO3",
      customerId: "customer-a",
      orderId: "order-1",
      amountVnd: 123000,
      expiresAt: new Date("2026-07-16T08:15:00.000Z")
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
      paymentTokenVersion: created.paymentTokenVersion,
      createdAt: {} as never,
      expiresAt: {
        toDate: () => new Date("2026-07-16T08:15:00.000Z")
      } as never,
      updatedAt: {} as never
    });

    expect(rebuilt.paymentPageUrl).toBe(created.paymentPageUrl);
    expect(rebuilt.qrPayload).toBe(created.qrPayload);
  });

  it("changes the payment page url when identity inputs change", () => {
    const provider = new DemoPaymentProvider(env);
    const base = provider.createSession({
      attemptId: "PTDEMO4",
      customerId: "customer-a",
      orderId: "order-1",
      amountVnd: 123000,
      expiresAt: new Date("2026-07-16T08:15:00.000Z")
    });
    const differentOrder = provider.createSession({
      attemptId: "PTDEMO4B",
      customerId: "customer-a",
      orderId: "order-2",
      amountVnd: 123000,
      expiresAt: new Date("2026-07-16T08:15:00.000Z")
    });
    const differentUser = provider.createSession({
      attemptId: "PTDEMO4C",
      customerId: "customer-b",
      orderId: "order-1",
      amountVnd: 123000,
      expiresAt: new Date("2026-07-16T08:15:00.000Z")
    });

    expect(base.paymentPageUrl).not.toBe(differentOrder.paymentPageUrl);
    expect(base.paymentPageUrl).not.toBe(differentUser.paymentPageUrl);
  });

  it("constant-time compare helper safely rejects unequal lengths", () => {
    expect(safeCompareHex("abcd", "abc")).toBe(false);
    expect(safeCompareHex(sha256Hex("left"), sha256Hex("right"))).toBe(false);
  });
});
