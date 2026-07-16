import express from "express";

import { createFirebaseAuthMiddleware } from "./auth/firebaseAuthMiddleware";
import type { FirebaseAuthVerifier } from "./auth/authenticatedRequest";
import { loadEnv, toSafeEnvSummary, type AppEnv } from "./config/env";
import { initializeFirebaseAdmin } from "./config/firebase";
import { createErrorHandler } from "./middleware/errorHandler";
import { createSimpleRateLimit } from "./middleware/rateLimit";
import { requestIdMiddleware } from "./middleware/requestId";
import { securityHeadersMiddleware } from "./middleware/securityHeaders";
import { FirestoreOrderRepository } from "./orders/orderRepository";
import { TrustedOrderAmountService } from "./orders/trustedOrderAmountService";
import { DemoPaymentPageService } from "./payments/demoPaymentPageService";
import { DemoPaymentProvider } from "./payments/demoPaymentProvider";
import { FirestorePaymentAttemptRepository } from "./payments/paymentAttemptRepository";
import { PaymentCreationService } from "./payments/paymentCreationService";
import { createDemoPaymentRoutes } from "./routes/demoPaymentRoutes";
import { createHealthRoutes } from "./routes/healthRoutes";
import { createPaymentCreateRoutes } from "./routes/paymentCreateRoutes";
import { SystemClock, type Clock } from "./util/clock";
import { sha256Hex } from "./util/hashing";
import { ConsoleSafeLogger, type SafeLogger } from "./util/safeLogger";

type AppDependencies = {
  env?: AppEnv;
  authVerifier?: FirebaseAuthVerifier;
  clock?: Clock;
  logger?: SafeLogger;
};

export function createApp(dependencies: AppDependencies = {}) {
  const env = dependencies.env ?? loadEnv();
  const logger = dependencies.logger ?? new ConsoleSafeLogger();
  logger.info("Payment backend configuration loaded", toSafeEnvSummary(env));

  const adminContext = initializeFirebaseAdmin(env);
  const orderRepository = new FirestoreOrderRepository(adminContext.firestore);
  const paymentAttemptRepository = new FirestorePaymentAttemptRepository(adminContext.firestore);
  const clock = dependencies.clock ?? new SystemClock();
  const trustedOrderAmountService = new TrustedOrderAmountService();
  const paymentProvider = new DemoPaymentProvider(env);
  const authVerifier =
    dependencies.authVerifier ??
    ({
      async verifyIdToken(token: string) {
        const decoded = await adminContext.auth.verifyIdToken(token, true);
        return { uid: decoded.uid };
      }
    } satisfies FirebaseAuthVerifier);

  const paymentCreationService = new PaymentCreationService(
    adminContext.firestore,
    orderRepository,
    paymentAttemptRepository,
    trustedOrderAmountService,
    paymentProvider,
    env.paymentSessionMinutes,
    clock
  );
  const demoPaymentPageService = new DemoPaymentPageService(
    adminContext.firestore,
    orderRepository,
    paymentAttemptRepository,
    trustedOrderAmountService,
    clock,
    logger,
    env
  );

  const app = express();
  app.disable("x-powered-by");
  app.use(securityHeadersMiddleware);
  app.use(requestIdMiddleware);
  app.use(express.json({ limit: "2kb" }));
  app.use(createHealthRoutes());
  app.use(
    "/api/v1/payments/create",
    createFirebaseAuthMiddleware(authVerifier),
    createSimpleRateLimit({
      limit: 10,
      windowMs: 60_000,
      keyFactory: (request) => {
        const principal = (request as { principal?: { uid?: string } }).principal;
        const uid = principal?.uid ?? "anonymous";
        return `${uid}:${request.ip}`;
      }
    }),
    createPaymentCreateRoutes(paymentCreationService)
  );
  app.use(
    "/demo/pay/:token/confirm",
    createSimpleRateLimit({
      limit: 20,
      windowMs: 60_000,
      keyFactory: (request) => {
        const token = Array.isArray(request.params.token)
          ? request.params.token[0] ?? "missing"
          : request.params.token ?? "missing";
        return `${sha256Hex(token)}:${request.ip}`;
      }
    })
  );
  app.use(
    "/demo/pay/:token/cancel",
    createSimpleRateLimit({
      limit: 20,
      windowMs: 60_000,
      keyFactory: (request) => {
        const token = Array.isArray(request.params.token)
          ? request.params.token[0] ?? "missing"
          : request.params.token ?? "missing";
        return `${sha256Hex(token)}:${request.ip}`;
      }
    })
  );
  app.use(createDemoPaymentRoutes(demoPaymentPageService));
  app.use(createErrorHandler(logger, env.nodeEnv));

  return { app, env };
}
