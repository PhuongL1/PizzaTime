import type { Firestore } from "firebase-admin/firestore";

import type { Clock } from "../util/clock";
import { sha256Hex } from "../util/hashing";
import type { AppEnv } from "../config/env";
import type { OrderRecord } from "../model/order";
import type { PaymentAttemptRecord } from "../model/paymentAttempt";
import type { FirestoreOrderRepository } from "../orders/orderRepository";
import type { TrustedOrderAmountService } from "../orders/trustedOrderAmountService";
import type { SafeLogger } from "../util/safeLogger";

import type { FirestorePaymentAttemptRepository } from "./paymentAttemptRepository";

export type DemoPaymentPageResponse = {
  statusCode: number;
  html: string;
};

export class DemoPaymentPageService {
  constructor(
    private readonly firestore: Firestore,
    private readonly orderRepository: FirestoreOrderRepository,
    private readonly paymentAttemptRepository: FirestorePaymentAttemptRepository,
    private readonly trustedOrderAmountService: TrustedOrderAmountService,
    private readonly clock: Clock,
    private readonly logger: SafeLogger,
    private readonly env: Pick<AppEnv, "appReturnDeepLinkBase">
  ) {}

  async renderPaymentPage(token: string): Promise<DemoPaymentPageResponse> {
    const tokenHash = toPaymentTokenHash(token);
    if (tokenHash === null) {
      return renderUnavailablePage(404, "Payment link unavailable");
    }

    const attempt = await this.paymentAttemptRepository.findByTokenHash(tokenHash);
    if (attempt === null) {
      return renderUnavailablePage(404, "Payment link unavailable");
    }

    const order = await this.orderRepository.getById(attempt.orderId);
    if (order === null) {
      return renderUnavailablePage(404, "Payment link unavailable");
    }

      return renderStatePage({
        attempt,
        order,
        now: this.clock.now(),
        token,
        mode: "view",
        appReturnDeepLinkBase: this.env.appReturnDeepLinkBase
      });
  }

  async confirmPayment(token: string): Promise<DemoPaymentPageResponse> {
    const tokenHash = toPaymentTokenHash(token);
    if (tokenHash === null) {
      return renderUnavailablePage(404, "Payment link unavailable");
    }

    return this.firestore.runTransaction(async (transaction) => {
      const attempt = await this.paymentAttemptRepository.findByTokenHashTx(transaction, tokenHash);
      if (attempt === null) {
        return renderUnavailablePage(404, "Payment link unavailable");
      }

      const order = await this.orderRepository.getByIdTx(transaction, attempt.orderId);
      if (order === null) {
        return renderUnavailablePage(404, "Payment link unavailable");
      }

      if (!this.amountStillMatches(order, attempt)) {
        this.logger.warn("Demo payment confirmation rejected because trusted amount no longer matches", {
          attemptId: attempt.id,
          orderId: order.id
        });
        return renderUnavailablePage(409, "Payment link unavailable");
      }

      if (attempt.status === "PAID" && order.paymentStatus === "PAID" && order.paymentAttemptId === attempt.id) {
        return renderStatePage({
          attempt,
          order,
          now: this.clock.now(),
          token,
          mode: "confirm",
          appReturnDeepLinkBase: this.env.appReturnDeepLinkBase
        });
      }

      if (order.paymentStatus === "PAID") {
        return renderUnavailablePage(409, "Payment has already been confirmed");
      }

      if (order.paymentAttemptId !== attempt.id) {
        return renderUnavailablePage(409, "Payment link is no longer active");
      }

      if (attempt.status !== "PENDING") {
        return renderStatePage({
          attempt,
          order,
          now: this.clock.now(),
          token,
          mode: "confirm",
          appReturnDeepLinkBase: this.env.appReturnDeepLinkBase
        });
      }

      if (attempt.expiresAt.toDate().getTime() <= this.clock.now().getTime()) {
        this.paymentAttemptRepository.markExpired(transaction, attempt.id);
        this.orderRepository.markFailedIfCurrent(transaction, order, attempt.id, "DEMO", "EXPIRED");
        return renderStatePage({
          attempt: {
            ...attempt,
            status: "EXPIRED"
          },
          order: {
            ...order,
            paymentStatus: order.paymentAttemptId === attempt.id ? "EXPIRED" : order.paymentStatus
          },
          now: this.clock.now(),
          token,
          mode: "confirm",
          appReturnDeepLinkBase: this.env.appReturnDeepLinkBase
        });
      }

      this.paymentAttemptRepository.markPaid(transaction, {
        attemptId: attempt.id,
        consumeToken: true
      });
      this.orderRepository.markPaid(transaction, {
        orderId: order.id,
        paymentAttemptId: attempt.id,
        paymentProvider: "DEMO",
        paymentReference: attempt.transactionRef
      });

      return renderStatePage({
        attempt: {
          ...attempt,
          status: "PAID"
        },
        order: {
          ...order,
          paymentStatus: "PAID",
          paymentProvider: "DEMO",
          paymentAttemptId: attempt.id,
          paymentReference: attempt.transactionRef
        },
        now: this.clock.now(),
        token,
        mode: "confirm",
        appReturnDeepLinkBase: this.env.appReturnDeepLinkBase
      });
    });
  }

  async cancelPayment(token: string): Promise<DemoPaymentPageResponse> {
    const tokenHash = toPaymentTokenHash(token);
    if (tokenHash === null) {
      return renderUnavailablePage(404, "Payment link unavailable");
    }

    return this.firestore.runTransaction(async (transaction) => {
      const attempt = await this.paymentAttemptRepository.findByTokenHashTx(transaction, tokenHash);
      if (attempt === null) {
        return renderUnavailablePage(404, "Payment link unavailable");
      }

      const order = await this.orderRepository.getByIdTx(transaction, attempt.orderId);
      if (order === null) {
        return renderUnavailablePage(404, "Payment link unavailable");
      }

      if (attempt.status === "PAID" || order.paymentStatus === "PAID") {
        return renderStatePage({
          attempt,
          order,
          now: this.clock.now(),
          token,
          mode: "cancel",
          appReturnDeepLinkBase: this.env.appReturnDeepLinkBase
        });
      }

      if (order.paymentAttemptId !== attempt.id) {
        return renderUnavailablePage(409, "Payment link is no longer active");
      }

      if (attempt.status !== "PENDING") {
        return renderStatePage({
          attempt,
          order,
          now: this.clock.now(),
          token,
          mode: "cancel",
          appReturnDeepLinkBase: this.env.appReturnDeepLinkBase
        });
      }

      if (attempt.expiresAt.toDate().getTime() <= this.clock.now().getTime()) {
        this.paymentAttemptRepository.markExpired(transaction, attempt.id);
        this.orderRepository.markFailedIfCurrent(transaction, order, attempt.id, "DEMO", "EXPIRED");
        return renderStatePage({
          attempt: {
            ...attempt,
            status: "EXPIRED"
          },
          order: {
            ...order,
            paymentStatus: order.paymentAttemptId === attempt.id ? "EXPIRED" : order.paymentStatus
          },
          now: this.clock.now(),
          token,
          mode: "cancel",
          appReturnDeepLinkBase: this.env.appReturnDeepLinkBase
        });
      }

      this.paymentAttemptRepository.markFailed(
        transaction,
        attempt.id,
        "CUSTOMER_CANCELLED",
        true
      );
      this.orderRepository.markFailedIfCurrent(transaction, order, attempt.id, "DEMO", "FAILED");

      return renderStatePage({
        attempt: {
          ...attempt,
          status: "FAILED",
          failureCode: "CUSTOMER_CANCELLED"
        },
        order: {
          ...order,
          paymentStatus: order.paymentAttemptId === attempt.id ? "FAILED" : order.paymentStatus
        },
        now: this.clock.now(),
        token,
        mode: "cancel",
        appReturnDeepLinkBase: this.env.appReturnDeepLinkBase
      });
    });
  }

  private amountStillMatches(order: OrderRecord, attempt: PaymentAttemptRecord): boolean {
    try {
      const trustedAmount = this.trustedOrderAmountService.resolve(order);
      return trustedAmount.amountVnd === attempt.amountVnd && attempt.providerAmount === attempt.amountVnd;
    } catch {
      return false;
    }
  }
}

function toPaymentTokenHash(token: string): string | null {
  if (!/^[A-Za-z0-9_-]{32,128}$/.test(token)) {
    return null;
  }
  return sha256Hex(token);
}

function renderStatePage(input: {
  attempt: PaymentAttemptRecord;
  order: OrderRecord;
  now: Date;
  token: string;
  mode: "view" | "confirm" | "cancel";
  appReturnDeepLinkBase: string | undefined;
}): DemoPaymentPageResponse {
  const orderReference = normalizeOrderReference(input.order.orderCode, input.order.id);
  const amountLabel = formatVnd(input.attempt.amountVnd);
  const paymentReference = escapeHtml(input.attempt.transactionRef);
  const confirmAction = `/demo/pay/${encodeURIComponent(input.token)}/confirm`;
  const cancelAction = `/demo/pay/${encodeURIComponent(input.token)}/cancel`;
  const returnToAppUrl = buildReturnToAppUrl(
    input.appReturnDeepLinkBase,
    input.order.id,
    input.attempt.id
  );

  let heading = "PizzaTime Demo Payment";
  let message = "For testing purposes only.";
  const subMessage = "No real money will be transferred.";
  let showButtons = false;

  if (input.attempt.status === "PAID" || input.order.paymentStatus === "PAID") {
    heading = "Demo payment confirmed";
    message = "This demo payment was already confirmed.";
  } else if (input.attempt.status === "FAILED") {
    heading = "Payment cancelled";
    message = "This payment was cancelled.";
  } else if (
    input.attempt.status === "EXPIRED" ||
    input.attempt.expiresAt.toDate().getTime() <= input.now.getTime()
  ) {
    heading = "Payment link expired";
    message = "This payment link has expired.";
  } else if (input.order.paymentAttemptId !== input.attempt.id) {
    heading = "Payment link inactive";
    message = "This payment link is no longer active.";
  } else {
    if (input.mode === "confirm") {
      heading = "Demo payment confirmed";
      message = "This demo payment has been confirmed.";
    } else {
      showButtons = true;
    }
  }

  return {
    statusCode: 200,
    html: `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(heading)}</title>
  <style>
    body { margin: 0; font-family: Arial, sans-serif; background: #f6f1e7; color: #241a12; }
    main { max-width: 520px; margin: 6vh auto; padding: 32px; background: #fffaf2; border-radius: 18px; box-shadow: 0 24px 60px rgba(36, 26, 18, 0.14); }
    h1 { margin-top: 0; font-size: 30px; }
    dl { margin: 24px 0; display: grid; grid-template-columns: 96px 1fr; row-gap: 12px; column-gap: 12px; }
    dt { font-weight: 700; }
    .notice { padding: 14px 16px; border-radius: 12px; background: #fff0cf; margin: 20px 0; }
    form { margin-top: 12px; }
    button { width: 100%; padding: 14px 16px; border: 0; border-radius: 12px; font-size: 16px; cursor: pointer; }
    .confirm { background: #1f6f3f; color: #fff; }
    .cancel { background: #efe1d7; color: #241a12; }
    .return-link { display: inline-block; margin-top: 16px; color: #1f6f3f; font-weight: 700; text-decoration: none; }
  </style>
</head>
<body>
  <main>
    <h1>${escapeHtml(heading)}</h1>
    <p>${escapeHtml(message)}</p>
    <p>${escapeHtml(subMessage)}</p>
    <dl>
      <dt>Order</dt>
      <dd>#${escapeHtml(orderReference)}</dd>
      <dt>Amount</dt>
      <dd>${escapeHtml(amountLabel)}</dd>
      <dt>Reference</dt>
      <dd>${paymentReference}</dd>
    </dl>
    <div class="notice">
      <strong>For testing purposes only.</strong><br>
      No real money will be transferred.
    </div>
    ${
      showButtons
        ? `<form method="post" action="${confirmAction}">
      <button class="confirm" type="submit">Confirm Demo Payment</button>
    </form>
    <form method="post" action="${cancelAction}">
      <button class="cancel" type="submit">Cancel Payment</button>
    </form>`
        : ""
    }
    ${
      returnToAppUrl === undefined || (input.mode === "view" && showButtons)
        ? ""
        : `<a class="return-link" href="${escapeHtml(returnToAppUrl)}">Return to PizzaTime</a>`
    }
  </main>
</body>
</html>`
  };
}

function renderUnavailablePage(statusCode: number, message: string): DemoPaymentPageResponse {
  return {
    statusCode,
    html: `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Payment link unavailable</title>
  <style>
    body { margin: 0; font-family: Arial, sans-serif; background: #f6f1e7; color: #241a12; }
    main { max-width: 520px; margin: 8vh auto; padding: 32px; background: #fffaf2; border-radius: 18px; box-shadow: 0 24px 60px rgba(36, 26, 18, 0.14); }
  </style>
</head>
<body>
  <main>
    <h1>Payment link unavailable</h1>
    <p>${escapeHtml(message)}</p>
    <p>For testing purposes only.</p>
    <p>No real money will be transferred.</p>
  </main>
</body>
</html>`
  };
}

function normalizeOrderReference(orderCode: string | undefined, orderId: string): string {
  const candidate = (orderCode ?? orderId).replace(/^#+/, "");
  const sanitized = candidate.replace(/[^A-Za-z0-9-]/g, "").slice(0, 32);
  return sanitized.length > 0 ? sanitized : orderId.slice(0, 12);
}

function formatVnd(amountVnd: number): string {
  return `${new Intl.NumberFormat("en-US").format(amountVnd)} VND`;
}

function escapeHtml(input: string): string {
  return input
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}

function buildReturnToAppUrl(
  base: string | undefined,
  orderId: string,
  paymentAttemptId: string
): string | undefined {
  if (base === undefined) {
    return undefined;
  }
  const url = new URL(base);
  url.searchParams.set("orderId", orderId);
  url.searchParams.set("paymentAttemptId", paymentAttemptId);
  return url.toString();
}
