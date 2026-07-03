import {initializeApp} from "firebase-admin/app";
import {getFirestore, FieldValue} from "firebase-admin/firestore";
import {getMessaging, MulticastMessage} from "firebase-admin/messaging";
import {logger} from "firebase-functions";
import {
  onDocumentCreated,
  onDocumentUpdated,
} from "firebase-functions/v2/firestore";

initializeApp();

const db = getFirestore();
const messaging = getMessaging();

const ORDER_STATUS_TYPE = "ORDER_STATUS";
const CUSTOMER_STATUSES = new Set([
  "CONFIRMED",
  "PREPARING",
  "BAKING",
  "READY",
  "ASSIGNED_TO_SHIPPER",
  "DELIVERING",
  "DELIVERED",
  "CANCELLED",
]);

type OrderData = {
  customerId?: string;
  status?: string;
};

type TokenRecipient = {
  uid: string;
  tokens: string[];
};

export const notifyStaffOnNewOrder = onDocumentCreated(
  "orders/{orderId}",
  async (event) => {
    const orderId = event.params.orderId;
    const order = event.data?.data() as OrderData | undefined;

    if (order?.status !== "PENDING") {
      logger.info("Skipping new order notification for non-pending order", {
        orderId,
        status: order?.status,
      });
      return;
    }

    const recipients = await getTokensByRole("STAFF");
    await sendToRecipients(
      recipients,
      "New order received",
      `Order #${shortOrderId(orderId)} is waiting for confirmation`,
      {
        orderId,
        status: "PENDING",
        type: ORDER_STATUS_TYPE,
      },
    );
  },
);

export const notifyOnOrderStatusChanged = onDocumentUpdated(
  "orders/{orderId}",
  async (event) => {
    const orderId = event.params.orderId;
    const before = event.data?.before.data() as OrderData | undefined;
    const after = event.data?.after.data() as OrderData | undefined;
    const oldStatus = before?.status;
    const newStatus = after?.status;

    if (!newStatus || oldStatus === newStatus) {
      logger.info("Skipping order notification because status did not change", {
        orderId,
        oldStatus,
        newStatus,
      });
      return;
    }

    const tasks: Promise<void>[] = [];

    if (CUSTOMER_STATUSES.has(newStatus) && after?.customerId) {
      tasks.push(
        getUserTokens(after.customerId).then((recipient) =>
          sendToRecipients(
            recipient ? [recipient] : [],
            "Order update",
            `Your order is now ${statusLabel(newStatus)}`,
            {
              orderId,
              status: newStatus,
              type: ORDER_STATUS_TYPE,
            },
          ),
        ),
      );
    }

    if (newStatus === "CONFIRMED") {
      tasks.push(
        getTokensByRole("KITCHEN").then((recipients) =>
          sendToRecipients(
            recipients,
            "Order ready for kitchen",
            `Order #${shortOrderId(orderId)} has been confirmed`,
            {
              orderId,
              status: newStatus,
              type: ORDER_STATUS_TYPE,
            },
          ),
        ),
      );
    }

    if (newStatus === "READY") {
      tasks.push(
        getTokensByRole("SHIPPER").then((recipients) =>
          sendToRecipients(
            recipients,
            "Order ready for delivery",
            `Order #${shortOrderId(orderId)} is ready for pickup`,
            {
              orderId,
              status: newStatus,
              type: ORDER_STATUS_TYPE,
            },
          ),
        ),
      );
    }

    await Promise.all(tasks);
  },
);

function shortOrderId(orderId: string): string {
  return orderId.length <= 8 ? orderId : orderId.slice(-8).toUpperCase();
}

function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    CONFIRMED: "confirmed",
    PREPARING: "being prepared",
    BAKING: "baking",
    READY: "ready",
    ASSIGNED_TO_SHIPPER: "assigned to a shipper",
    DELIVERING: "out for delivery",
    DELIVERED: "delivered",
    CANCELLED: "cancelled",
  };
  return labels[status] ?? status.toLowerCase().replace(/_/g, " ");
}

async function getUserTokens(uid: string): Promise<TokenRecipient | null> {
  const snapshot = await db.collection("users").doc(uid).get();
  const tokens = readTokens(snapshot.get("fcmTokens"));
  return tokens.length > 0 ? {uid, tokens} : null;
}

async function getTokensByRole(role: string): Promise<TokenRecipient[]> {
  const snapshot = await db
    .collection("users")
    .where("role", "==", role)
    .where("active", "==", true)
    .get();

  return snapshot.docs
    .map((doc) => ({
      uid: doc.id,
      tokens: readTokens(doc.get("fcmTokens")),
    }))
    .filter((recipient) => recipient.tokens.length > 0);
}

function readTokens(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.filter((token): token is string => token.trim().length > 0);
}

async function sendToRecipients(
  recipients: TokenRecipient[],
  title: string,
  body: string,
  data: Record<string, string>,
): Promise<void> {
  const tokenOwners = new Map<string, string>();
  const tokens = recipients.flatMap((recipient) =>
    recipient.tokens.map((token) => {
      tokenOwners.set(token, recipient.uid);
      return token;
    }),
  );

  if (tokens.length === 0) {
    logger.info("No FCM tokens found for notification", {title, data});
    return;
  }

  const invalidTokensByUid = new Map<string, string[]>();
  for (const tokenBatch of chunk(tokens, 500)) {
    const message: MulticastMessage = {
      tokens: tokenBatch,
      notification: {
        title,
        body,
      },
      data,
      android: {
        priority: "high",
      },
    };

    const response = await messaging.sendEachForMulticast(message);
    logger.info("Sent FCM notification batch", {
      title,
      successCount: response.successCount,
      failureCount: response.failureCount,
      data,
    });

    response.responses.forEach((sendResponse, index) => {
      if (sendResponse.success) {
        return;
      }

      const token = tokenBatch[index];
      const code = sendResponse.error?.code;
      if (isInvalidTokenError(code)) {
        const uid = tokenOwners.get(token);
        if (uid) {
          const invalidTokens = invalidTokensByUid.get(uid) ?? [];
          invalidTokens.push(token);
          invalidTokensByUid.set(uid, invalidTokens);
        }
      }

      logger.warn("FCM send failed", {
        code,
        message: sendResponse.error?.message,
      });
    });
  }

  await removeInvalidTokens(invalidTokensByUid);
}

function chunk<T>(items: T[], size: number): T[][] {
  const batches: T[][] = [];
  for (let index = 0; index < items.length; index += size) {
    batches.push(items.slice(index, index + size));
  }
  return batches;
}

function isInvalidTokenError(code: string | undefined): boolean {
  return code === "messaging/registration-token-not-registered" ||
    code === "messaging/invalid-registration-token";
}

async function removeInvalidTokens(
  invalidTokensByUid: Map<string, string[]>,
): Promise<void> {
  const updates = Array.from(invalidTokensByUid.entries()).map(
    ([uid, tokens]) =>
      db.collection("users").doc(uid).update({
        fcmTokens: FieldValue.arrayRemove(...tokens),
      }),
  );

  if (updates.length > 0) {
    await Promise.all(updates);
    logger.info("Removed invalid FCM tokens", {userCount: updates.length});
  }
}
