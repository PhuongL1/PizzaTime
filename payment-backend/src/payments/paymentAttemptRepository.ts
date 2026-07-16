import {
  FieldValue,
  Timestamp,
  type QuerySnapshot,
  type Firestore,
  type Transaction
} from "firebase-admin/firestore";

import type { PaymentAttemptRecord } from "../model/paymentAttempt";
import type { PaymentProviderCode } from "../model/paymentProvider";
import { paymentAttemptDocumentSchema } from "../model/paymentAttempt";
import { sha256Hex } from "../util/hashing";

type CreateAttemptInput = {
  id: string;
  orderId: string;
  customerId: string;
  requestIdHash: string;
  amountVnd: number;
  providerAmount: number;
  paymentProvider: PaymentProviderCode;
  paymentTokenHash: string;
  paymentTokenSalt: string;
  createdAt: Date;
  expiresAt: Date;
};

export class FirestorePaymentAttemptRepository {
  private readonly collection;

  constructor(firestore: Firestore) {
    this.collection = firestore.collection("paymentAttempts");
  }

  static buildAttemptId(customerId: string, orderId: string, requestId: string): string {
    return `PT${sha256Hex(`${customerId}|${orderId}|${requestId}`).slice(0, 30).toUpperCase()}`;
  }

  async getById(attemptId: string): Promise<PaymentAttemptRecord | null> {
    const snapshot = await this.collection.doc(attemptId).get();
    if (!snapshot.exists) {
      return null;
    }
    return toPaymentAttemptRecord(snapshot.id, snapshot.data() ?? {});
  }

  async getByIdTx(transaction: Transaction, attemptId: string): Promise<PaymentAttemptRecord | null> {
    const snapshot = await transaction.get(this.collection.doc(attemptId));
    if (!snapshot.exists) {
      return null;
    }
    return toPaymentAttemptRecord(snapshot.id, snapshot.data() ?? {});
  }

  async findByTokenHash(tokenHash: string): Promise<PaymentAttemptRecord | null> {
    const snapshot = await this.collection.where("paymentTokenHash", "==", tokenHash).limit(2).get();
    return this.toSingleRecord(snapshot);
  }

  async findByTokenHashTx(
    transaction: Transaction,
    tokenHash: string
  ): Promise<PaymentAttemptRecord | null> {
    const snapshot = await transaction.get(
      this.collection.where("paymentTokenHash", "==", tokenHash).limit(2)
    );
    return this.toSingleRecord(snapshot);
  }

  createPendingAttempt(transaction: Transaction, input: CreateAttemptInput): void {
    transaction.set(this.collection.doc(input.id), {
      schemaVersion: 1,
      provider: input.paymentProvider,
      status: "PENDING",
      orderId: input.orderId,
      customerId: input.customerId,
      transactionRef: input.id,
      requestIdHash: input.requestIdHash,
      amountVnd: input.amountVnd,
      providerAmount: input.providerAmount,
      currency: "VND",
      paymentTokenHash: input.paymentTokenHash,
      paymentTokenSalt: input.paymentTokenSalt,
      createdAt: Timestamp.fromDate(input.createdAt),
      expiresAt: Timestamp.fromDate(input.expiresAt),
      updatedAt: Timestamp.fromDate(input.createdAt)
    });
  }

  markExpired(transaction: Transaction, attemptId: string): void {
    transaction.update(this.collection.doc(attemptId), {
      status: "EXPIRED",
      tokenConsumedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp()
    });
  }

  markFailed(
    transaction: Transaction,
    attemptId: string,
    failureCode: string,
    consumeToken: boolean
  ): void {
    transaction.update(this.collection.doc(attemptId), {
      status: "FAILED",
      failureCode,
      ...(consumeToken ? { tokenConsumedAt: FieldValue.serverTimestamp() } : {}),
      updatedAt: FieldValue.serverTimestamp()
    });
  }

  markPaid(transaction: Transaction, input: { attemptId: string; consumeToken: boolean }): void {
    transaction.update(this.collection.doc(input.attemptId), {
      status: "PAID",
      confirmedAt: FieldValue.serverTimestamp(),
      ...(input.consumeToken ? { tokenConsumedAt: FieldValue.serverTimestamp() } : {}),
      updatedAt: FieldValue.serverTimestamp()
    });
  }

  private toSingleRecord(snapshot: QuerySnapshot): PaymentAttemptRecord | null {
    if (snapshot.empty) {
      return null;
    }
    if (snapshot.docs.length !== 1) {
      throw new Error("Unexpected number of payment attempts matched the token hash.");
    }
    const document = snapshot.docs[0];
    if (document === undefined) {
      throw new Error("Payment attempt query returned no documents.");
    }
    return toPaymentAttemptRecord(document.id, document.data() ?? {});
  }
}

function toPaymentAttemptRecord(id: string, data: Record<string, unknown>): PaymentAttemptRecord {
  const parsed = paymentAttemptDocumentSchema.parse(data);
  return {
    id,
    schemaVersion: parsed.schemaVersion,
    provider: parsed.provider,
    status: parsed.status,
    orderId: parsed.orderId,
    customerId: parsed.customerId,
    transactionRef: parsed.transactionRef,
    requestIdHash: parsed.requestIdHash,
    amountVnd: parsed.amountVnd,
    providerAmount: parsed.providerAmount,
    currency: parsed.currency,
    paymentTokenHash: parsed.paymentTokenHash,
    paymentTokenSalt: parsed.paymentTokenSalt,
    createdAt: parsed.createdAt,
    expiresAt: parsed.expiresAt,
    updatedAt: parsed.updatedAt,
    ...(parsed.confirmedAt === undefined ? {} : { confirmedAt: parsed.confirmedAt }),
    ...(parsed.providerTransactionId === undefined
      ? {}
      : { providerTransactionId: parsed.providerTransactionId }),
    ...(parsed.failureCode === undefined ? {} : { failureCode: parsed.failureCode }),
    ...(parsed.tokenConsumedAt === undefined ? {} : { tokenConsumedAt: parsed.tokenConsumedAt })
  };
}
