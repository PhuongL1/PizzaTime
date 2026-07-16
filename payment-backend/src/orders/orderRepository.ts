import {
  FieldValue,
  type DocumentData,
  type Firestore,
  type Transaction
} from "firebase-admin/firestore";

import type { OrderRecord } from "../model/order";
import type { PaymentProviderCode } from "../model/paymentProvider";
import { trustedPricingSnapshotSchema } from "../model/order";

export class FirestoreOrderRepository {
  private readonly collection;

  constructor(firestore: Firestore) {
    this.collection = firestore.collection("orders");
  }

  async getById(orderId: string): Promise<OrderRecord | null> {
    const snapshot = await this.collection.doc(orderId).get();
    if (!snapshot.exists) {
      return null;
    }
    return toOrderRecord(snapshot.id, snapshot.data() ?? {});
  }

  async getByIdTx(transaction: Transaction, orderId: string): Promise<OrderRecord | null> {
    const snapshot = await transaction.get(this.collection.doc(orderId));
    if (!snapshot.exists) {
      return null;
    }
    return toOrderRecord(snapshot.id, snapshot.data() ?? {});
  }

  setActivePaymentAttempt(
    transaction: Transaction,
    input: {
      orderId: string;
      paymentMethod: PaymentProviderCode;
      paymentProvider: PaymentProviderCode;
      paymentAttemptId: string;
      paymentReference: string;
    }
  ): void {
    transaction.update(this.collection.doc(input.orderId), {
      paymentMethod: input.paymentMethod,
      paymentStatus: "PENDING",
      paymentProvider: input.paymentProvider,
      paymentAttemptId: input.paymentAttemptId,
      paymentReference: input.paymentReference,
      updatedAt: FieldValue.serverTimestamp()
    });
  }

  markExpiredIfCurrent(
    transaction: Transaction,
    order: OrderRecord,
    paymentAttemptId: string,
    paymentProvider: PaymentProviderCode
  ): void {
    if (order.paymentStatus === "PAID" || order.paymentAttemptId !== paymentAttemptId) {
      return;
    }
    transaction.update(this.collection.doc(order.id), {
      paymentStatus: "EXPIRED",
      paymentAttemptId,
      paymentReference: paymentAttemptId,
      paymentProvider,
      updatedAt: FieldValue.serverTimestamp()
    });
  }

  markFailedIfCurrent(
    transaction: Transaction,
    order: OrderRecord,
    paymentAttemptId: string,
    paymentProvider: PaymentProviderCode,
    failureStatus: "FAILED" | "EXPIRED" = "FAILED"
  ): void {
    if (order.paymentStatus === "PAID" || order.paymentAttemptId !== paymentAttemptId) {
      return;
    }
    transaction.update(this.collection.doc(order.id), {
      paymentStatus: failureStatus,
      paymentProvider,
      paymentAttemptId,
      paymentReference: paymentAttemptId,
      updatedAt: FieldValue.serverTimestamp()
    });
  }

  markPaid(
    transaction: Transaction,
    input: {
      orderId: string;
      paymentAttemptId: string;
      paymentProvider: PaymentProviderCode;
      paymentReference: string;
      providerTransactionId?: string;
    }
  ): void {
    transaction.update(this.collection.doc(input.orderId), {
      paymentStatus: "PAID",
      paymentProvider: input.paymentProvider,
      paymentAttemptId: input.paymentAttemptId,
      paymentReference: input.paymentReference,
      ...(input.providerTransactionId === undefined
        ? {}
        : { providerTransactionId: input.providerTransactionId }),
      paidAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp()
    });
  }
}

function toOrderRecord(id: string, data: DocumentData): OrderRecord {
  const rawData = data as Record<string, unknown>;
  const snapshotCandidate = rawData.pricingSnapshotVnd;
  const snapshot = trustedPricingSnapshotSchema.safeParse(snapshotCandidate);
  const paymentProvider = stringOrUndefined(data.paymentProvider);
  const paymentAttemptId = stringOrUndefined(data.paymentAttemptId);
  const paymentReference = stringOrUndefined(data.paymentReference);
  const providerTransactionId = stringOrUndefined(data.providerTransactionId);
  const finalTotal = numberOrUndefined(data.finalTotal);
  const total = numberOrUndefined(data.total);
  const orderCode = stringOrUndefined(data.orderCode);

  return {
    id,
    ...(orderCode === undefined ? {} : { orderCode }),
    customerId: stringOrEmpty(data.customerId),
    status: stringOrEmpty(data.status),
    paymentMethod: stringOrEmpty(data.paymentMethod),
    paymentStatus: toPaymentStatus(data.paymentStatus),
    ...(paymentProvider === undefined ? {} : { paymentProvider }),
    ...(paymentAttemptId === undefined ? {} : { paymentAttemptId }),
    ...(paymentReference === undefined ? {} : { paymentReference }),
    ...(providerTransactionId === undefined ? {} : { providerTransactionId }),
    ...(finalTotal === undefined ? {} : { finalTotal }),
    ...(total === undefined ? {} : { total }),
    ...(snapshot.success ? { pricingSnapshotVnd: snapshot.data } : {}),
    raw: rawData
  };
}

function stringOrEmpty(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function stringOrUndefined(value: unknown): string | undefined {
  return typeof value === "string" && value.length > 0 ? value : undefined;
}

function numberOrUndefined(value: unknown): number | undefined {
  return typeof value === "number" ? value : undefined;
}

function toPaymentStatus(value: unknown): OrderRecord["paymentStatus"] {
  if (typeof value !== "string" || value.length === 0) {
    return "UNKNOWN";
  }
  return value as OrderRecord["paymentStatus"];
}
