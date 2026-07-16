import type { Firestore } from "firebase-admin/firestore";

import type { Clock } from "../util/clock";
import { HttpError } from "../util/httpError";
import { sha256Hex } from "../util/hashing";
import { assertCustomerOwnsOrder, assertOrderPayable } from "../orders/orderPaymentPolicy";
import type { FirestoreOrderRepository } from "../orders/orderRepository";
import type { TrustedOrderAmountService } from "../orders/trustedOrderAmountService";

import { FirestorePaymentAttemptRepository } from "./paymentAttemptRepository";
import type { PaymentProvider } from "./paymentProvider";

export type CreatePaymentInput = {
  customerId: string;
  orderId: string;
  requestId: string;
};

export type CreatePaymentOutput = {
  paymentAttemptId: string;
  paymentReference: string;
  paymentPageUrl: string;
  qrPayload: string;
  amountVnd: number;
  expiresAt: string;
};

export class PaymentCreationService {
  constructor(
    private readonly firestore: Firestore,
    private readonly orderRepository: FirestoreOrderRepository,
    private readonly paymentAttemptRepository: FirestorePaymentAttemptRepository,
    private readonly trustedOrderAmountService: TrustedOrderAmountService,
    private readonly paymentProvider: PaymentProvider,
    private readonly paymentSessionMinutes: number,
    private readonly clock: Clock
  ) {}

  async createPayment(input: CreatePaymentInput): Promise<CreatePaymentOutput> {
    const now = this.clock.now();
    return this.firestore.runTransaction(async (transaction) => {
      const order = await this.orderRepository.getByIdTx(transaction, input.orderId);
      if (order === null) {
        throw new HttpError(404, "ORDER_NOT_FOUND", "Order not found.");
      }
      assertCustomerOwnsOrder(order, input.customerId);
      assertOrderPayable(order, this.paymentProvider.paymentMethod);

      if (order.paymentAttemptId !== undefined) {
        const activeAttempt = await this.paymentAttemptRepository.getByIdTx(
          transaction,
          order.paymentAttemptId
        );
        if (activeAttempt !== null) {
          if (activeAttempt.status === "PAID") {
            throw new HttpError(409, "ORDER_ALREADY_PAID", "Order has already been paid.");
          }
          if (activeAttempt.status === "PENDING") {
            if (activeAttempt.expiresAt.toDate().getTime() > now.getTime()) {
              return this.rebuildCreatePaymentOutput(activeAttempt);
            }
            this.paymentAttemptRepository.markExpired(transaction, activeAttempt.id);
            this.orderRepository.markExpiredIfCurrent(
              transaction,
              order,
              activeAttempt.id,
              this.paymentProvider.code
            );
          }
        }
      }

      const attemptId = FirestorePaymentAttemptRepository.buildAttemptId(
        input.customerId,
        input.orderId,
        input.requestId
      );
      const existingAttempt = await this.paymentAttemptRepository.getByIdTx(transaction, attemptId);
      if (existingAttempt !== null) {
        if (existingAttempt.status === "PENDING" && existingAttempt.expiresAt.toDate() > now) {
          return this.rebuildCreatePaymentOutput(existingAttempt);
        }
        if (existingAttempt.status === "PAID") {
          throw new HttpError(409, "ORDER_ALREADY_PAID", "Order has already been paid.");
        }
        throw new HttpError(
          409,
          "PAYMENT_ATTEMPT_FINALIZED",
          "This requestId maps to a finalized payment attempt."
        );
      }

      const trustedAmount = this.trustedOrderAmountService.resolve(order);
      const expiresAt = new Date(now.getTime() + this.paymentSessionMinutes * 60_000);
      const requestIdHash = sha256Hex(`${input.customerId}|${input.orderId}|${input.requestId}`);
      const session = this.paymentProvider.createSession({
        attemptId,
        customerId: input.customerId,
        orderId: input.orderId,
        amountVnd: trustedAmount.amountVnd,
        expiresAt
      });

      this.paymentAttemptRepository.createPendingAttempt(transaction, {
        id: attemptId,
        orderId: order.id,
        customerId: input.customerId,
        requestIdHash,
        amountVnd: trustedAmount.amountVnd,
        providerAmount: session.providerAmount,
        paymentProvider: this.paymentProvider.code,
        paymentTokenHash: session.paymentTokenHash,
        paymentTokenVersion: session.paymentTokenVersion,
        createdAt: now,
        expiresAt
      });
      this.orderRepository.setActivePaymentAttempt(transaction, {
        orderId: order.id,
        paymentMethod: this.paymentProvider.paymentMethod,
        paymentProvider: this.paymentProvider.code,
        paymentAttemptId: attemptId,
        paymentReference: session.paymentReference
      });

      return {
        paymentAttemptId: attemptId,
        paymentReference: session.paymentReference,
        paymentPageUrl: session.paymentPageUrl,
        qrPayload: session.qrPayload,
        amountVnd: trustedAmount.amountVnd,
        expiresAt: expiresAt.toISOString()
      };
    });
  }

  private rebuildCreatePaymentOutput(
    attempt: Awaited<ReturnType<FirestorePaymentAttemptRepository["getById"]>> extends infer T
      ? NonNullable<T>
      : never
  ): CreatePaymentOutput {
    const session = this.paymentProvider.rebuildSession(attempt);
    return {
      paymentAttemptId: attempt.id,
      paymentReference: session.paymentReference,
      paymentPageUrl: session.paymentPageUrl,
      qrPayload: session.qrPayload,
      amountVnd: attempt.amountVnd,
      expiresAt: attempt.expiresAt.toDate().toISOString()
    };
  }
}
