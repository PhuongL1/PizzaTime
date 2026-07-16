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

  it("ignores unrelated process environment variables", () => {
    const env = loadEnv({
      ...baseEnv,
      PATH: "C:\\Windows\\System32",
      APPDATA: "C:\\Users\\tester\\AppData\\Roaming",
      USERNAME: "tester",
      npm_config_cache: "C:\\Users\\tester\\AppData\\Local\\npm-cache"
    });

    expect(env.publicBaseUrl).toBe("https://payments.example.test");
  });

  it("does not treat GOOGLE_APPLICATION_CREDENTIALS as app config", () => {
    const env = loadEnv({
      ...baseEnv,
      GOOGLE_APPLICATION_CREDENTIALS: "D:\\secrets\\serviceAccountKey.json"
    });

    expect(env.firebaseProjectId).toBe("demo-project");
  });

  it("rejects genuinely missing backend variables", () => {
    expect(() => loadEnv({ ...baseEnv, FIREBASE_PROJECT_ID: undefined })).toThrow();
    expect(() => loadEnv({ ...baseEnv, PUBLIC_BASE_URL: undefined })).toThrow();
    expect(() => loadEnv({ ...baseEnv, DEMO_PAYMENT_TOKEN_SECRET: undefined })).toThrow();
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
    const summary = toSafeEnvSummary(env);

    expect(summary).toEqual({
      nodeEnv: "development",
      port: 8080,
      firebaseProjectId: "demo-project",
      publicBaseUrl: "https://payments.example.test",
      paymentProvider: "DEMO",
      demoPaymentEnabled: true,
      paymentSessionMinutes: 15
    });
    expect(summary).not.toHaveProperty("demoPaymentTokenSecret");
  });

  it("accepts test values through dependency injection", () => {
    const env = loadEnv({
      ...baseEnv,
      NODE_ENV: "test",
      PUBLIC_BASE_URL: "http://127.0.0.1:8080"
    });

    expect(env).toMatchObject({
      nodeEnv: "test",
      publicBaseUrl: "http://127.0.0.1:8080"
    });
  });

  it("accepts the exact payment-result deep link base", () => {
    const env = loadEnv({
      ...baseEnv,
      APP_RETURN_DEEP_LINK_BASE: "pizzatime://payment-result"
    });

    expect(env.appReturnDeepLinkBase).toBe("pizzatime://payment-result");
  });

  it("rejects an invalid payment-result deep link base", () => {
    expect(() =>
      loadEnv({
        ...baseEnv,
        APP_RETURN_DEEP_LINK_BASE: "https://example.test/payment-result"
      })
    ).toThrow("APP_RETURN_DEEP_LINK_BASE must use the pizzatime scheme.");
  });
});
