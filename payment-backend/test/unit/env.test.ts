import { describe, expect, it } from "vitest";

import { loadEnv, toSafeEnvSummary } from "../../src/config/env";

const baseEnv = {
  NODE_ENV: "development",
  PORT: "8080",
  FIREBASE_PROJECT_ID: "demo-project",
  PAYMENT_PROVIDER: "DEMO",
  DEMO_PAYMENT_ENABLED: "true",
  DEMO_PAYMENT_TOKEN_SECRET: "demo-secret-demo-secret-demo-secret-1234",
  PUBLIC_BASE_URL: "https://payments.example.test",
  PAYMENT_SESSION_MINUTES: "15",
  APP_RETURN_DEEP_LINK_BASE: ""
} satisfies Record<string, string>;

describe("env", () => {
  it("rejects missing demo token secret", () => {
    expect(() => loadEnv({ ...baseEnv, DEMO_PAYMENT_TOKEN_SECRET: "" })).toThrow();
  });

  it("rejects invalid public base url", () => {
    expect(() => loadEnv({ ...baseEnv, PUBLIC_BASE_URL: "not-a-url" })).toThrow();
  });

  it("rejects http public base url outside test mode", () => {
    expect(() =>
      loadEnv({ ...baseEnv, NODE_ENV: "production", PUBLIC_BASE_URL: "http://example.test" })
    ).toThrow("PUBLIC_BASE_URL must use https outside test mode.");
  });

  it("rejects demo provider in production mode", () => {
    expect(() => loadEnv({ ...baseEnv, NODE_ENV: "production" })).toThrow(
      "DEMO payment provider cannot run in production."
    );
  });

  it("rejects disabled demo provider", () => {
    expect(() => loadEnv({ ...baseEnv, DEMO_PAYMENT_ENABLED: "false" })).toThrow(
      "DEMO payment provider is disabled."
    );
  });

  it("omits secrets from safe summaries", () => {
    const env = loadEnv(baseEnv);
    expect(toSafeEnvSummary(env)).toEqual({
      nodeEnv: "development",
      port: 8080,
      firebaseProjectId: "demo-project",
      publicBaseUrl: "https://payments.example.test",
      paymentProvider: "DEMO",
      demoPaymentEnabled: true,
      paymentSessionMinutes: 15
    });
  });
});
