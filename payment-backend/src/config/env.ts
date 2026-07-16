import { z } from "zod";

const nodeEnvSchema = z.enum(["development", "test", "production"]);
const paymentProviderSchema = z.enum(["DEMO"]);

const rawEnvSchema = z
  .object({
    NODE_ENV: nodeEnvSchema.default("development"),
    PORT: z.coerce.number().int().min(1).max(65535).default(8080),
    FIREBASE_PROJECT_ID: z.string().trim().min(1),
    PUBLIC_BASE_URL: z.string().trim().min(1),
    PAYMENT_PROVIDER: paymentProviderSchema.default("DEMO"),
    DEMO_PAYMENT_ENABLED: z.enum(["true", "false"]).default("false"),
    DEMO_PAYMENT_TOKEN_SECRET: z.string().trim().min(32),
    PAYMENT_SESSION_MINUTES: z.coerce.number().int().min(1).max(120).default(15),
    APP_RETURN_DEEP_LINK_BASE: z.string().trim().optional().default("")
  })
  .strict();

export type AppEnv = {
  nodeEnv: "development" | "test" | "production";
  port: number;
  firebaseProjectId: string;
  publicBaseUrl: string;
  paymentProvider: "DEMO";
  demoPaymentEnabled: boolean;
  demoPaymentTokenSecret: string;
  paymentSessionMinutes: number;
  appReturnDeepLinkBase?: string;
};

export function loadEnv(rawEnv: Record<string, string | undefined> = process.env): AppEnv {
  const parsed = rawEnvSchema.parse(rawEnv);
  const demoPaymentEnabled = parsed.DEMO_PAYMENT_ENABLED === "true";
  const publicBaseUrl = new URL(parsed.PUBLIC_BASE_URL);
  if (parsed.NODE_ENV !== "test" && publicBaseUrl.protocol !== "https:") {
    throw new Error("PUBLIC_BASE_URL must use https outside test mode.");
  }
  if (parsed.PAYMENT_PROVIDER === "DEMO" && demoPaymentEnabled === false) {
    throw new Error("DEMO payment provider is disabled.");
  }
  if (parsed.NODE_ENV === "production" && demoPaymentEnabled) {
    throw new Error("DEMO payment provider cannot run in production.");
  }
  const deepLinkBase =
    parsed.APP_RETURN_DEEP_LINK_BASE.length === 0
      ? undefined
      : validateAppReturnDeepLinkBase(parsed.APP_RETURN_DEEP_LINK_BASE);

  return {
    nodeEnv: parsed.NODE_ENV,
    port: parsed.PORT,
    firebaseProjectId: parsed.FIREBASE_PROJECT_ID,
    publicBaseUrl: publicBaseUrl.toString().replace(/\/$/, ""),
    paymentProvider: parsed.PAYMENT_PROVIDER,
    demoPaymentEnabled,
    demoPaymentTokenSecret: parsed.DEMO_PAYMENT_TOKEN_SECRET,
    paymentSessionMinutes: parsed.PAYMENT_SESSION_MINUTES,
    ...(deepLinkBase === undefined ? {} : { appReturnDeepLinkBase: deepLinkBase })
  };
}

function validateAppReturnDeepLinkBase(value: string): string {
  const url = new URL(value);
  if (url.protocol !== "pizzatime:") {
    throw new Error("APP_RETURN_DEEP_LINK_BASE must use the pizzatime scheme.");
  }
  if (url.hostname !== "payment-result") {
    throw new Error("APP_RETURN_DEEP_LINK_BASE must target pizzatime://payment-result.");
  }
  if (url.username.length > 0 || url.password.length > 0) {
    throw new Error("APP_RETURN_DEEP_LINK_BASE must not include credentials.");
  }
  if (url.pathname !== "" && url.pathname !== "/") {
    throw new Error("APP_RETURN_DEEP_LINK_BASE must not include a path.");
  }
  if (url.search.length > 0 || url.hash.length > 0) {
    throw new Error("APP_RETURN_DEEP_LINK_BASE must not include a query or fragment.");
  }
  return "pizzatime://payment-result";
}

export function toSafeEnvSummary(env: AppEnv): Record<string, string | number | boolean> {
  return {
    nodeEnv: env.nodeEnv,
    port: env.port,
    firebaseProjectId: env.firebaseProjectId,
    publicBaseUrl: env.publicBaseUrl,
    paymentProvider: env.paymentProvider,
    demoPaymentEnabled: env.demoPaymentEnabled,
    paymentSessionMinutes: env.paymentSessionMinutes,
    ...(env.appReturnDeepLinkBase === undefined
      ? {}
      : { appReturnDeepLinkBase: env.appReturnDeepLinkBase })
  };
}
