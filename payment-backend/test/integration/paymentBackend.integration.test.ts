import { getApps, initializeApp } from "firebase-admin/app";
import {
  Timestamp,
  type QueryDocumentSnapshot,
  getFirestore
} from "firebase-admin/firestore";
import request from "supertest";
import { beforeAll, beforeEach, describe, expect, it } from "vitest";
import { z } from "zod";

import { createApp } from "../../src/app";
import type { AppEnv } from "../../src/config/env";
import type { Clock } from "../../src/util/clock";

const env: AppEnv = {
  nodeEnv: "test",
  port: 8080,
  firebaseProjectId: "demo-pizzatime-payment",
  paymentProvider: "DEMO",
  demoPaymentEnabled: true,
  demoPaymentTokenSecret: "demo-secret-demo-secret-demo-secret-1234",
  publicBaseUrl: "https://payments.example.test",
  paymentSessionMinutes: 15,
  appReturnDeepLinkBase: "pizzatime://payment-result"
};

const fakeAuthVerifier = {
  verifyIdToken(token: string) {
    if (token === "valid-token") {
      return Promise.resolve({ uid: "customer-a" });
    }
    if (token === "other-token") {
      return Promise.resolve({ uid: "customer-b" });
    }
    throw new Error("invalid token");
  }
};

const createPaymentResponseSchema = z.object({
  paymentAttemptId: z.string(),
  paymentReference: z.string(),
  paymentPageUrl: z.string().url(),
  qrPayload: z.string().url(),
  amountVnd: z.number(),
  expiresAt: z.string()
});

const errorResponseSchema = z.object({
  error: z.object({
    code: z.string(),
    message: z.string()
  })
});
const paidAttemptStateSchema = z.object({
  status: z.string(),
  confirmedAt: z.unknown().optional(),
  tokenConsumedAt: z.unknown().optional()
});

beforeAll(() => {
  if (getApps().length === 0) {
    initializeApp({
      projectId: env.firebaseProjectId
    });
  }
});

beforeEach(async () => {
  const firestore = getFirestore();
  await clearCollection("paymentAttempts");
  await clearCollection("orders");
  await clearCollection("users");
  await firestore.doc("users/customer-a").set({ role: "CUSTOMER", active: true });
  await firestore.doc("users/customer-b").set({ role: "CUSTOMER", active: true });
});

describe("payment backend integration", () => {
  it("creates one idempotent demo payment attempt and ignores client-supplied amount fields", async () => {
    await seedTrustedOrder("de-3001");
    const { app } = createApp({
      env,
      authVerifier: fakeAuthVerifier
    });

    const first = await request(app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer valid-token")
      .send({
        orderId: "de-3001",
        requestId: "req-3001-abcdef",
        amount: 1,
        customerId: "customer-b"
      });
    const firstBody = createPaymentResponseSchema.parse(first.body);

    expect(first.status).toBe(200);
    expect(firstBody.paymentAttemptId).toMatch(/^PT/);
    expect(firstBody.paymentReference).toBe(firstBody.paymentAttemptId);
    expect(firstBody.amountVnd).toBe(123000);
    expect(firstBody.paymentPageUrl).toContain("/demo/pay/");
    expect(firstBody.qrPayload).toBe(firstBody.paymentPageUrl);

    const second = await request(app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer valid-token")
      .send({
        orderId: "de-3001",
        requestId: "req-3001-abcdef"
      });
    const secondBody = createPaymentResponseSchema.parse(second.body);

    expect(second.status).toBe(200);
    expect(secondBody.paymentAttemptId).toBe(firstBody.paymentAttemptId);
    expect(secondBody.paymentPageUrl).toBe(firstBody.paymentPageUrl);

    const token = extractToken(firstBody.paymentPageUrl);
    const attemptSnapshot = await getFirestore()
      .doc(`paymentAttempts/${firstBody.paymentAttemptId}`)
      .get();
    expect(attemptSnapshot.exists).toBe(true);
    expect(attemptSnapshot.data()?.provider).toBe("DEMO");
    expect(attemptSnapshot.data()?.paymentTokenHash).not.toBe(token);
    expect(attemptSnapshot.data()?.paymentToken).toBeUndefined();

    const orderSnapshot = await getFirestore().doc("orders/de-3001").get();
    expect(orderSnapshot.data()?.paymentAttemptId).toBe(firstBody.paymentAttemptId);
    expect(orderSnapshot.data()?.paymentStatus).toBe("PENDING");
    expect(orderSnapshot.data()?.paymentMethod).toBe("DEMO");
  });

  it("requires authenticated customer ownership", async () => {
    await seedTrustedOrder("de-3002");
    const { app } = createApp({
      env,
      authVerifier: fakeAuthVerifier
    });

    const forbidden = await request(app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer other-token")
      .send({
        orderId: "de-3002",
        requestId: "req-3002-abcdef"
      });
    const forbiddenBody = errorResponseSchema.parse(forbidden.body);
    expect(forbidden.status).toBe(403);
    expect(forbiddenBody.error.code).toBe("ORDER_FORBIDDEN");

    const unauthenticated = await request(app)
      .post("/api/v1/payments/create")
      .send({
        orderId: "de-3002",
        requestId: "req-3002-abcdef"
      });
    expect(unauthenticated.status).toBe(401);
  });

  it("GET demo payment page performs no write and renders the testing warning", async () => {
    await seedTrustedOrder("de-3003");
    const { app } = createApp({
      env,
      authVerifier: fakeAuthVerifier
    });

    const createResponse = await request(app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer valid-token")
      .send({
        orderId: "de-3003",
        requestId: "req-3003-abcdef"
      });
    const createBody = createPaymentResponseSchema.parse(createResponse.body);
    const token = extractToken(createBody.paymentPageUrl);

    const beforeOrder = await getFirestore().doc("orders/de-3003").get();
    const beforeAttempt = await getFirestore()
      .doc(`paymentAttempts/${createBody.paymentAttemptId}`)
      .get();

    const page = await request(app).get(`/demo/pay/${token}`);
    expect(page.status).toBe(200);
    expect(page.headers["cache-control"]).toBe("no-store");
    expect(page.text).toContain("PizzaTime Demo Payment");
    expect(page.text).toContain("Confirm Demo Payment");
    expect(page.text).toContain("Cancel Payment");
    expect(page.text).toContain("For testing purposes only");
    expect(page.text).toContain("No real money will be transferred");
    expect(page.text).not.toContain("users/customer-a");
    expect(page.text).not.toContain("Return to PizzaTime");

    const afterOrder = await getFirestore().doc("orders/de-3003").get();
    const afterAttempt = await getFirestore()
      .doc(`paymentAttempts/${createBody.paymentAttemptId}`)
      .get();
    expect(afterOrder.data()?.paymentStatus).toBe(beforeOrder.data()?.paymentStatus);
    expect(afterAttempt.data()?.status).toBe(beforeAttempt.data()?.status);
  });

  it("confirm changes PENDING to PAID atomically and remains idempotent on repeat", async () => {
    await seedTrustedOrder("de-3004");
    const { app } = createApp({
      env,
      authVerifier: fakeAuthVerifier
    });

    const createResponse = await request(app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer valid-token")
      .send({
        orderId: "de-3004",
        requestId: "req-3004-abcdef"
      });
    const createBody = createPaymentResponseSchema.parse(createResponse.body);
    const token = extractToken(createBody.paymentPageUrl);

    const firstConfirm = await request(app).post(`/demo/pay/${token}/confirm`);
    expect(firstConfirm.status).toBe(200);
    expect(firstConfirm.text).toContain("Demo payment confirmed");
    expect(firstConfirm.text).toContain("Return to PizzaTime");
    expect(firstConfirm.text).toContain(
      `pizzatime://payment-result?orderId=de-3004&amp;paymentAttemptId=${createBody.paymentAttemptId}`
    );
    expect(firstConfirm.text).not.toContain("status=");
    expect(firstConfirm.text).not.toContain("amountVnd");

    const orderSnapshot = await getFirestore().doc("orders/de-3004").get();
    expect(orderSnapshot.data()?.paymentStatus).toBe("PAID");
    expect(orderSnapshot.data()?.paymentProvider).toBe("DEMO");
    expect(orderSnapshot.data()?.status).toBe("PENDING");
    expect(orderSnapshot.data()?.deliveryHandoffStatus).toBe("LOCKED");

    const attemptRef = getFirestore().doc(`paymentAttempts/${createBody.paymentAttemptId}`);
    const firstAttempt = await attemptRef.get();
    const firstAttemptData = paidAttemptStateSchema.parse(firstAttempt.data() ?? {});
    expect(firstAttemptData.status).toBe("PAID");
    const firstConfirmedAt = firstAttemptData.confirmedAt;
    const firstConsumedAt = firstAttemptData.tokenConsumedAt;

    const secondConfirm = await request(app).post(`/demo/pay/${token}/confirm`);
    expect(secondConfirm.status).toBe(200);
    expect(secondConfirm.text).toContain("already confirmed");

    const secondAttempt = await attemptRef.get();
    const secondAttemptData = paidAttemptStateSchema.parse(secondAttempt.data() ?? {});
    expect(secondAttemptData.confirmedAt).toEqual(firstConfirmedAt);
    expect(secondAttemptData.tokenConsumedAt).toEqual(firstConsumedAt);
  });

  it("cancel changes only the active pending attempt to FAILED and never overwrites PAID", async () => {
    await seedTrustedOrder("de-3005");
    const { app } = createApp({
      env,
      authVerifier: fakeAuthVerifier
    });

    const cancelledCreate = await request(app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer valid-token")
      .send({
        orderId: "de-3005",
        requestId: "req-3005-cancel"
      });
    const cancelledBody = createPaymentResponseSchema.parse(cancelledCreate.body);
    const cancelledToken = extractToken(cancelledBody.paymentPageUrl);

    const cancelResponse = await request(app).post(`/demo/pay/${cancelledToken}/cancel`);
    expect(cancelResponse.status).toBe(200);
    expect(cancelResponse.text).toContain("Payment cancelled");
    expect(cancelResponse.text).toContain("Return to PizzaTime");

    const cancelledOrder = await getFirestore().doc("orders/de-3005").get();
    expect(cancelledOrder.data()?.paymentStatus).toBe("FAILED");
    const cancelledAttempt = await getFirestore()
      .doc(`paymentAttempts/${cancelledBody.paymentAttemptId}`)
      .get();
    expect(cancelledAttempt.data()?.status).toBe("FAILED");
    expect(cancelledAttempt.data()?.failureCode).toBe("CUSTOMER_CANCELLED");

    await seedTrustedOrder("de-3006");
    const paidCreate = await request(app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer valid-token")
      .send({
        orderId: "de-3006",
        requestId: "req-3006-paid"
      });
    const paidBody = createPaymentResponseSchema.parse(paidCreate.body);
    const paidToken = extractToken(paidBody.paymentPageUrl);

    await request(app).post(`/demo/pay/${paidToken}/confirm`);
    const cancelAfterPaid = await request(app).post(`/demo/pay/${paidToken}/cancel`);
    expect(cancelAfterPaid.status).toBe(200);
    expect(cancelAfterPaid.text).toContain("already confirmed");

    const paidOrder = await getFirestore().doc("orders/de-3006").get();
    expect(paidOrder.data()?.paymentStatus).toBe("PAID");
  });

  it("expired token cannot confirm and marks the current attempt expired", async () => {
    await seedTrustedOrder("de-3007");
    const createClock = new FixedClock("2026-07-16T08:00:00.000Z");
    const laterClock = new FixedClock("2026-07-16T08:20:00.000Z");

    const createAppResult = createApp({
      env,
      authVerifier: fakeAuthVerifier,
      clock: createClock
    });
    const createResponse = await request(createAppResult.app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer valid-token")
      .send({
        orderId: "de-3007",
        requestId: "req-3007-abcdef"
      });
    const createBody = createPaymentResponseSchema.parse(createResponse.body);
    const token = extractToken(createBody.paymentPageUrl);

    const confirmApp = createApp({
      env,
      authVerifier: fakeAuthVerifier,
      clock: laterClock
    });
    const expiredResponse = await request(confirmApp.app).post(`/demo/pay/${token}/confirm`);
    expect(expiredResponse.status).toBe(200);
    expect(expiredResponse.text).toContain("Payment link expired");

    const orderSnapshot = await getFirestore().doc("orders/de-3007").get();
    expect(orderSnapshot.data()?.paymentStatus).toBe("EXPIRED");
    const attemptSnapshot = await getFirestore()
      .doc(`paymentAttempts/${createBody.paymentAttemptId}`)
      .get();
    expect(attemptSnapshot.data()?.status).toBe("EXPIRED");
  });

  it("an old token cannot confirm after a newer attempt supersedes it", async () => {
    await seedTrustedOrder("de-3008");
    const { app } = createApp({
      env,
      authVerifier: fakeAuthVerifier
    });

    const firstCreate = await request(app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer valid-token")
      .send({
        orderId: "de-3008",
        requestId: "req-3008-old"
      });
    const firstBody = createPaymentResponseSchema.parse(firstCreate.body);
    const firstToken = extractToken(firstBody.paymentPageUrl);
    await request(app).post(`/demo/pay/${firstToken}/cancel`);

    const secondCreate = await request(app)
      .post("/api/v1/payments/create")
      .set("Authorization", "Bearer valid-token")
      .send({
        orderId: "de-3008",
        requestId: "req-3008-new"
      });
    const secondBody = createPaymentResponseSchema.parse(secondCreate.body);

    const staleConfirm = await request(app).post(`/demo/pay/${firstToken}/confirm`);
    expect(staleConfirm.status).toBe(409);
    expect(staleConfirm.text).not.toContain("This demo payment has been confirmed.");

    const orderSnapshot = await getFirestore().doc("orders/de-3008").get();
    expect(orderSnapshot.data()?.paymentAttemptId).toBe(secondBody.paymentAttemptId);
    expect(orderSnapshot.data()?.paymentStatus).toBe("PENDING");
  });

  it("malformed token fails safely without writes", async () => {
    await seedTrustedOrder("de-3009");
    const { app } = createApp({
      env,
      authVerifier: fakeAuthVerifier
    });

    const response = await request(app).post("/demo/pay/not-valid!!!/confirm");
    expect(response.status).toBe(404);
    expect(response.text).toContain("Payment link unavailable");

    const attempts = await getFirestore().collection("paymentAttempts").get();
    expect(attempts.empty).toBe(true);
  });
});

async function seedTrustedOrder(orderId: string): Promise<void> {
  await getFirestore().doc(`orders/${orderId}`).set({
    orderId,
    orderCodeKey: orderId,
    orderCode: `#${orderId}`,
    customerId: "customer-a",
    status: "PENDING",
    paymentMethod: "DEMO",
    paymentStatus: "PENDING",
    deliveryHandoffStatus: "LOCKED",
    total: 123000,
    finalTotal: 123000,
    pricingSnapshotVnd: {
      schemaVersion: 1,
      currency: "VND",
      itemsSubtotalVnd: 120000,
      discountVnd: 0,
      deliveryFeeVnd: 3000,
      totalVnd: 123000
    },
    createdAt: Timestamp.now(),
    updatedAt: Timestamp.now()
  });
}

function extractToken(paymentPageUrl: string): string {
  const url = new URL(paymentPageUrl);
  return url.pathname.split("/").at(-1) ?? "";
}

async function clearCollection(collectionPath: string): Promise<void> {
  const collection = getFirestore().collection(collectionPath);
  const snapshots = await collection.get();
  await Promise.all(
    snapshots.docs.map((snapshot: QueryDocumentSnapshot) => snapshot.ref.delete())
  );
}

class FixedClock implements Clock {
  constructor(private readonly value: string) {}

  now(): Date {
    return new Date(this.value);
  }
}
