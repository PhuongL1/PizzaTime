import { after, before, beforeEach, describe, test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  GeoPoint,
  Timestamp,
  deleteDoc,
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
  updateDoc,
} from "firebase/firestore";

const PROJECT_ID = "demo-pizzatime-rules";
const OWNER_CUSTOMER_ID = "customer-owner";
const OTHER_CUSTOMER_ID = "customer-other";
const ASSIGNED_SHIPPER_ID = "shipper-assigned";
const OTHER_SHIPPER_ID = "shipper-other";
const STAFF_ID = "staff-user";
const KITCHEN_ID = "kitchen-user";
const ADMIN_ID = "admin-user";
const ORDER_COD_PENDING = "co-1001";
const ORDER_DEMO_PENDING = "de-1001";
const ORDER_DEMO_PAID = "de-1002";
const ORDER_DEMO_DELIVERING_LOCKED = "de-1003";
const ORDER_VNPAY_PENDING = "vp-1001";
const ORDER_VNPAY_PAID = "vp-1002";
const ORDER_VNPAY_DELIVERING_LOCKED = "vp-1003";
const ORDER_VNPAY_AWAITING = "vp-1004";
const ORDER_VNPAY_CONFIRMED = "vp-1005";
const ORDER_COD_DELIVERING = "co-1002";
const ORDER_DELIVERED = "co-1003";
const LEGACY_COD_ORDER = "le-1001";

let testEnvironment;

function userContext(userId) {
  return testEnvironment.authenticatedContext(userId).firestore();
}

function orderDoc(userId, orderId) {
  return doc(userContext(userId), `orders/${orderId}`);
}

function trackingDoc(userId, orderId = ORDER_VNPAY_DELIVERING_LOCKED) {
  return doc(userContext(userId), `orders/${orderId}/tracking/current`);
}

function paymentAttemptDoc(userId, attemptId = "attempt-demo-1") {
  return doc(userContext(userId), `paymentAttempts/${attemptId}`);
}

function history(status, actorRole, actorId) {
  return [
    {
      status,
      actorRole,
      actorId,
      note: `${status} event`,
      createdAt: Timestamp.now(),
    },
  ];
}

function baseCreateOrder(orderId, overrides = {}) {
  return {
    customerId: OWNER_CUSTOMER_ID,
    customerEmail: "owner@example.com",
    customerName: "Owner Customer",
    customerPhone: "0123456789",
    storeName: "PizzaTime",
    pickupAddress: "1 Pizza Street",
    pickupLat: 10.762622,
    pickupLng: 106.660172,
    storePhone: "0987654321",
    status: "PENDING",
    orderType: "DELIVERY",
    paymentMethod: "COD",
    paymentStatus: "NOT_REQUIRED",
    cashCollected: false,
    deliveryHandoffStatus: "NOT_REQUIRED",
    deliveryAddress: "123 Delivery Street",
    deliveryLocation: new GeoPoint(10.7769, 106.7009),
    distanceKm: 2.5,
    itemsSubtotal: 20,
    subtotal: 20,
    deliveryFee: 2.5,
    discountAmount: 0,
    discount: 0,
    promoCode: "",
    finalTotal: 22.5,
    total: 22.5,
    note: "",
    items: [
      {
        productId: "pizza-1",
        name: "Margherita",
        quantity: 1,
        unitPrice: 20,
        totalPrice: 20,
      },
    ],
    statusHistory: history("PENDING", "CUSTOMER", OWNER_CUSTOMER_ID),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    orderId,
    orderCodeKey: orderId,
    orderCode: `#${orderId}`,
    ...overrides,
  };
}

function seededOrder(overrides = {}) {
  return {
    customerId: OWNER_CUSTOMER_ID,
    customerEmail: "owner@example.com",
    customerName: "Owner Customer",
    customerPhone: "0123456789",
    storeName: "PizzaTime",
    pickupAddress: "1 Pizza Street",
    pickupLat: 10.762622,
    pickupLng: 106.660172,
    storePhone: "0987654321",
    status: "PENDING",
    orderType: "DELIVERY",
    paymentMethod: "COD",
    paymentStatus: "NOT_REQUIRED",
    cashCollected: false,
    deliveryHandoffStatus: "NOT_REQUIRED",
    deliveryAddress: "123 Delivery Street",
    deliveryLocation: new GeoPoint(10.7769, 106.7009),
    distanceKm: 2.5,
    itemsSubtotal: 20,
    subtotal: 20,
    deliveryFee: 2.5,
    discountAmount: 0,
    discount: 0,
    promoCode: "",
    finalTotal: 22.5,
    total: 22.5,
    note: "",
    items: [
      {
        productId: "pizza-1",
        name: "Margherita",
        quantity: 1,
        unitPrice: 20,
        totalPrice: 20,
      },
    ],
    statusHistory: history("PENDING", "CUSTOMER", OWNER_CUSTOMER_ID),
    createdAt: Timestamp.now(),
    updatedAt: Timestamp.now(),
    orderId: ORDER_COD_PENDING,
    orderCodeKey: ORDER_COD_PENDING,
    orderCode: `#${ORDER_COD_PENDING}`,
    ...overrides,
  };
}

function trackingPayload(overrides = {}) {
  return {
    shipperId: ASSIGNED_SHIPPER_ID,
    location: new GeoPoint(10.7769, 106.7009),
    accuracyMeters: 18.5,
    bearingDegrees: 125.0,
    speedMetersPerSecond: 7.25,
    recordedAt: Timestamp.now(),
    updatedAt: serverTimestamp(),
    orderStatus: "DELIVERING",
    schemaVersion: 1,
    ...overrides,
  };
}

async function seedFixtureData() {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const firestore = context.firestore();
    const users = [
      [OWNER_CUSTOMER_ID, "CUSTOMER"],
      [OTHER_CUSTOMER_ID, "CUSTOMER"],
      [ASSIGNED_SHIPPER_ID, "SHIPPER"],
      [OTHER_SHIPPER_ID, "SHIPPER"],
      [STAFF_ID, "STAFF"],
      [KITCHEN_ID, "KITCHEN"],
      [ADMIN_ID, "ADMIN"],
    ];

    for (const [userId, role] of users) {
      await setDoc(doc(firestore, `users/${userId}`), {
        id: userId,
        role,
        active: true,
      });
    }

    await setDoc(doc(firestore, `orders/${LEGACY_COD_ORDER}`), {
      customerId: OWNER_CUSTOMER_ID,
      status: "PENDING",
      orderId: LEGACY_COD_ORDER,
      orderCodeKey: LEGACY_COD_ORDER,
      orderCode: `#${LEGACY_COD_ORDER}`,
    });
    await setDoc(doc(firestore, `orders/${ORDER_COD_PENDING}`), seededOrder({
      orderId: ORDER_COD_PENDING,
      orderCodeKey: ORDER_COD_PENDING,
      orderCode: `#${ORDER_COD_PENDING}`,
    }));
    await setDoc(doc(firestore, `orders/${ORDER_DEMO_PENDING}`), seededOrder({
      orderId: ORDER_DEMO_PENDING,
      orderCodeKey: ORDER_DEMO_PENDING,
      orderCode: `#${ORDER_DEMO_PENDING}`,
      paymentMethod: "DEMO",
      paymentStatus: "PENDING",
      deliveryHandoffStatus: "LOCKED",
    }));
    await setDoc(doc(firestore, `orders/${ORDER_DEMO_PAID}`), seededOrder({
      orderId: ORDER_DEMO_PAID,
      orderCodeKey: ORDER_DEMO_PAID,
      orderCode: `#${ORDER_DEMO_PAID}`,
      paymentMethod: "DEMO",
      paymentStatus: "PAID",
      deliveryHandoffStatus: "LOCKED",
    }));
    await setDoc(doc(firestore, `orders/${ORDER_DEMO_DELIVERING_LOCKED}`), seededOrder({
      orderId: ORDER_DEMO_DELIVERING_LOCKED,
      orderCodeKey: ORDER_DEMO_DELIVERING_LOCKED,
      orderCode: `#${ORDER_DEMO_DELIVERING_LOCKED}`,
      status: "DELIVERING",
      shipperId: ASSIGNED_SHIPPER_ID,
      paymentMethod: "DEMO",
      paymentStatus: "PAID",
      deliveryHandoffStatus: "LOCKED",
      statusHistory: history("DELIVERING", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
    await setDoc(doc(firestore, `orders/${ORDER_VNPAY_PENDING}`), seededOrder({
      orderId: ORDER_VNPAY_PENDING,
      orderCodeKey: ORDER_VNPAY_PENDING,
      orderCode: `#${ORDER_VNPAY_PENDING}`,
      paymentMethod: "VNPAY",
      paymentStatus: "PENDING",
      deliveryHandoffStatus: "LOCKED",
    }));
    await setDoc(doc(firestore, `orders/${ORDER_VNPAY_PAID}`), seededOrder({
      orderId: ORDER_VNPAY_PAID,
      orderCodeKey: ORDER_VNPAY_PAID,
      orderCode: `#${ORDER_VNPAY_PAID}`,
      paymentMethod: "VNPAY",
      paymentStatus: "PAID",
      deliveryHandoffStatus: "LOCKED",
    }));
    await setDoc(doc(firestore, `orders/${ORDER_VNPAY_DELIVERING_LOCKED}`), seededOrder({
      orderId: ORDER_VNPAY_DELIVERING_LOCKED,
      orderCodeKey: ORDER_VNPAY_DELIVERING_LOCKED,
      orderCode: `#${ORDER_VNPAY_DELIVERING_LOCKED}`,
      status: "DELIVERING",
      shipperId: ASSIGNED_SHIPPER_ID,
      paymentMethod: "VNPAY",
      paymentStatus: "PAID",
      deliveryHandoffStatus: "LOCKED",
      statusHistory: history("DELIVERING", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
    await setDoc(doc(firestore, `orders/${ORDER_VNPAY_AWAITING}`), seededOrder({
      orderId: ORDER_VNPAY_AWAITING,
      orderCodeKey: ORDER_VNPAY_AWAITING,
      orderCode: `#${ORDER_VNPAY_AWAITING}`,
      status: "DELIVERING",
      shipperId: ASSIGNED_SHIPPER_ID,
      paymentMethod: "VNPAY",
      paymentStatus: "PAID",
      deliveryHandoffStatus: "AWAITING_CUSTOMER",
      shipperArrivedAt: Timestamp.now(),
      statusHistory: history("DELIVERING", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
    await setDoc(doc(firestore, `orders/${ORDER_VNPAY_CONFIRMED}`), seededOrder({
      orderId: ORDER_VNPAY_CONFIRMED,
      orderCodeKey: ORDER_VNPAY_CONFIRMED,
      orderCode: `#${ORDER_VNPAY_CONFIRMED}`,
      status: "DELIVERING",
      shipperId: ASSIGNED_SHIPPER_ID,
      paymentMethod: "VNPAY",
      paymentStatus: "PAID",
      deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
      shipperArrivedAt: Timestamp.now(),
      customerReceivedAt: Timestamp.now(),
      customerReceiptConfirmedBy: OWNER_CUSTOMER_ID,
      statusHistory: history("DELIVERING", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
    await setDoc(doc(firestore, `orders/${ORDER_COD_DELIVERING}`), seededOrder({
      orderId: ORDER_COD_DELIVERING,
      orderCodeKey: ORDER_COD_DELIVERING,
      orderCode: `#${ORDER_COD_DELIVERING}`,
      status: "DELIVERING",
      shipperId: ASSIGNED_SHIPPER_ID,
      statusHistory: history("DELIVERING", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
    await setDoc(doc(firestore, `orders/${ORDER_DELIVERED}`), seededOrder({
      orderId: ORDER_DELIVERED,
      orderCodeKey: ORDER_DELIVERED,
      orderCode: `#${ORDER_DELIVERED}`,
      status: "DELIVERED",
      shipperId: ASSIGNED_SHIPPER_ID,
      cashCollected: true,
      deliveredAt: Timestamp.now(),
      collectedByShipperId: ASSIGNED_SHIPPER_ID,
      collectedAmount: 22.5,
      statusHistory: history("DELIVERED", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
    await setDoc(doc(firestore, `orders/${ORDER_VNPAY_DELIVERING_LOCKED}/tracking/current`), {
      ...trackingPayload(),
      updatedAt: Timestamp.now(),
    });
    await setDoc(doc(firestore, "paymentAttempts/attempt-demo-1"), {
      schemaVersion: 1,
      provider: "DEMO",
      status: "PENDING",
      orderId: ORDER_DEMO_PENDING,
      customerId: OWNER_CUSTOMER_ID,
      transactionRef: "attempt-demo-1",
      requestIdHash: "hash-demo-1",
      amountVnd: 123000,
      providerAmount: 123000,
      currency: "VND",
      paymentTokenHash: "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      paymentTokenSalt: "salt-demo-1",
      createdAt: Timestamp.now(),
      expiresAt: Timestamp.now(),
      updatedAt: Timestamp.now(),
    });
  });
}

async function assertOrderCreateSucceeds(orderId, payload) {
  await assertSucceeds(setDoc(orderDoc(OWNER_CUSTOMER_ID, orderId), payload));
}

async function assertOrderCreateFails(orderId, payload) {
  await assertFails(setDoc(orderDoc(OWNER_CUSTOMER_ID, orderId), payload));
}

describe("PizzaTime Firestore rules for payment handoff and tracking", () => {
  before(async () => {
    const rules = readFileSync(new URL("../../firestore.rules", import.meta.url), "utf8");
    testEnvironment = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host: "127.0.0.1",
        port: 8080,
        rules,
      },
    });
  });

  beforeEach(async () => {
    await testEnvironment.clearFirestore();
    await seedFixtureData();
  });

  after(async () => {
    await testEnvironment.cleanup();
  });

  test("legacy COD order remains readable", async () => {
    await assertSucceeds(getDoc(orderDoc(OWNER_CUSTOMER_ID, LEGACY_COD_ORDER)));
  });

  test("Customer can create valid COD initialization", async () => {
    await assertOrderCreateSucceeds("co-2001", baseCreateOrder("co-2001"));
  });

  test("Customer cannot create COD order as PAID", async () => {
    await assertOrderCreateFails("co-2002", baseCreateOrder("co-2002", {
      paymentStatus: "PAID",
    }));
  });

  test("Customer can create future VNPAY order only as PENDING plus LOCKED", async () => {
    await assertOrderCreateSucceeds("vp-2001", baseCreateOrder("vp-2001", {
      paymentMethod: "VNPAY",
      paymentStatus: "PENDING",
      deliveryHandoffStatus: "LOCKED",
    }));
  });

  test("Customer can create DEMO prepaid order only as PENDING plus LOCKED", async () => {
    await assertOrderCreateSucceeds("de-2001", baseCreateOrder("de-2001", {
      paymentMethod: "DEMO",
      paymentStatus: "PENDING",
      deliveryHandoffStatus: "LOCKED",
    }));
  });

  test("Customer cannot create VNPAY order already PAID", async () => {
    await assertOrderCreateFails("vp-2002", baseCreateOrder("vp-2002", {
      paymentMethod: "VNPAY",
      paymentStatus: "PAID",
      deliveryHandoffStatus: "LOCKED",
    }));
  });

  test("Customer cannot initialize CUSTOMER_CONFIRMED", async () => {
    await assertOrderCreateFails("vp-2003", baseCreateOrder("vp-2003", {
      paymentMethod: "VNPAY",
      paymentStatus: "PENDING",
      deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
    }));
  });

  test("Customer cannot initialize DELIVERED", async () => {
    await assertOrderCreateFails("vp-2004", baseCreateOrder("vp-2004", {
      status: "DELIVERED",
    }));
  });

  test("Customer cannot change PENDING to PAID", async () => {
    await assertFails(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_PENDING), {
      paymentStatus: "PAID",
    }));
  });

  test("Staff cannot change PENDING to PAID", async () => {
    await assertFails(updateDoc(orderDoc(STAFF_ID, ORDER_VNPAY_PENDING), {
      paymentStatus: "PAID",
    }));
  });

  test("Shipper cannot change PENDING to PAID", async () => {
    await assertFails(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_PENDING), {
      paymentStatus: "PAID",
    }));
  });

  test("Customer cannot change provider transaction fields", async () => {
    await assertFails(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_PENDING), {
      paymentReference: "provider-ref",
    }));
  });

  test("Client cannot alter paidAt", async () => {
    await assertFails(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_PENDING), {
      paidAt: serverTimestamp(),
    }));
  });

  test("Staff can confirm valid COD order according to existing rules", async () => {
    await assertSucceeds(updateDoc(orderDoc(STAFF_ID, ORDER_COD_PENDING), {
      status: "CONFIRMED",
      updatedAt: serverTimestamp(),
      statusHistory: [
        ...history("PENDING", "CUSTOMER", OWNER_CUSTOMER_ID),
        {
          status: "CONFIRMED",
          actorRole: "STAFF",
          actorId: STAFF_ID,
          note: "Order confirmed",
          createdAt: Timestamp.now(),
        },
      ],
    }));
  });

  test("Staff cannot confirm unpaid VNPAY order", async () => {
    await assertFails(updateDoc(orderDoc(STAFF_ID, ORDER_VNPAY_PENDING), {
      status: "CONFIRMED",
      updatedAt: serverTimestamp(),
      statusHistory: history("CONFIRMED", "STAFF", STAFF_ID),
    }));
  });

  test("Staff can confirm paid VNPAY order", async () => {
    await assertSucceeds(updateDoc(orderDoc(STAFF_ID, ORDER_VNPAY_PAID), {
      status: "CONFIRMED",
      updatedAt: serverTimestamp(),
      statusHistory: history("CONFIRMED", "STAFF", STAFF_ID),
    }));
  });

  test("Staff can confirm paid DEMO order", async () => {
    await assertSucceeds(updateDoc(orderDoc(STAFF_ID, ORDER_DEMO_PAID), {
      status: "CONFIRMED",
      updatedAt: serverTimestamp(),
      statusHistory: history("CONFIRMED", "STAFF", STAFF_ID),
    }));
  });

  test("Assigned Shipper can mark DEMO LOCKED to AWAITING_CUSTOMER while DELIVERING", async () => {
    await assertSucceeds(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_DEMO_DELIVERING_LOCKED), {
      deliveryHandoffStatus: "AWAITING_CUSTOMER",
      shipperArrivedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }));
  });

  test("Assigned Shipper can mark LOCKED to AWAITING_CUSTOMER while DELIVERING", async () => {
    await assertSucceeds(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_DELIVERING_LOCKED), {
      deliveryHandoffStatus: "AWAITING_CUSTOMER",
      shipperArrivedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }));
  });

  test("Other Shipper denied arrival", async () => {
    await assertFails(updateDoc(orderDoc(OTHER_SHIPPER_ID, ORDER_VNPAY_DELIVERING_LOCKED), {
      deliveryHandoffStatus: "AWAITING_CUSTOMER",
      shipperArrivedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }));
  });

  test("Customer denied arrival", async () => {
    await assertFails(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_DELIVERING_LOCKED), {
      deliveryHandoffStatus: "AWAITING_CUSTOMER",
      shipperArrivedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }));
  });

  test("Assigned Shipper denied arrival before DELIVERING", async () => {
    await assertFails(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_PAID), {
      deliveryHandoffStatus: "AWAITING_CUSTOMER",
      shipperArrivedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }));
  });

  test("Assigned Shipper denied arrival when unpaid", async () => {
    await assertFails(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_PENDING), {
      deliveryHandoffStatus: "AWAITING_CUSTOMER",
      shipperArrivedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }));
  });

  test("Extra field change denied during arrival", async () => {
    await assertFails(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_DELIVERING_LOCKED), {
      deliveryHandoffStatus: "AWAITING_CUSTOMER",
      shipperArrivedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
      note: "extra",
    }));
  });

  test("Owning Customer can mark AWAITING_CUSTOMER to CUSTOMER_CONFIRMED", async () => {
    await assertSucceeds(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_AWAITING), {
      deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
      customerReceivedAt: serverTimestamp(),
      customerReceiptConfirmedBy: OWNER_CUSTOMER_ID,
      updatedAt: serverTimestamp(),
    }));
  });

  test("Other Customer denied receipt confirmation", async () => {
    await assertFails(updateDoc(orderDoc(OTHER_CUSTOMER_ID, ORDER_VNPAY_AWAITING), {
      deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
      customerReceivedAt: serverTimestamp(),
      customerReceiptConfirmedBy: OTHER_CUSTOMER_ID,
      updatedAt: serverTimestamp(),
    }));
  });

  test("Shipper denied receipt confirmation", async () => {
    await assertFails(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_AWAITING), {
      deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
      customerReceivedAt: serverTimestamp(),
      customerReceiptConfirmedBy: OWNER_CUSTOMER_ID,
      updatedAt: serverTimestamp(),
    }));
  });

  test("Customer denied when handoff is LOCKED", async () => {
    await assertFails(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_DELIVERING_LOCKED), {
      deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
      customerReceivedAt: serverTimestamp(),
      customerReceiptConfirmedBy: OWNER_CUSTOMER_ID,
      updatedAt: serverTimestamp(),
    }));
  });

  test("Customer denied after CANCELLED", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), `orders/${ORDER_VNPAY_AWAITING}`), seededOrder({
        orderId: ORDER_VNPAY_AWAITING,
        orderCodeKey: ORDER_VNPAY_AWAITING,
        orderCode: `#${ORDER_VNPAY_AWAITING}`,
        status: "CANCELLED",
        shipperId: ASSIGNED_SHIPPER_ID,
        paymentMethod: "VNPAY",
        paymentStatus: "PAID",
        deliveryHandoffStatus: "AWAITING_CUSTOMER",
      }));
    });
    await assertFails(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_AWAITING), {
      deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
      customerReceivedAt: serverTimestamp(),
      customerReceiptConfirmedBy: OWNER_CUSTOMER_ID,
      updatedAt: serverTimestamp(),
    }));
  });

  test("Mismatched confirmedBy denied", async () => {
    await assertFails(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_AWAITING), {
      deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
      customerReceivedAt: serverTimestamp(),
      customerReceiptConfirmedBy: OTHER_CUSTOMER_ID,
      updatedAt: serverTimestamp(),
    }));
  });

  test("Extra field change denied during customer confirmation", async () => {
    await assertFails(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_AWAITING), {
      deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
      customerReceivedAt: serverTimestamp(),
      customerReceiptConfirmedBy: OWNER_CUSTOMER_ID,
      updatedAt: serverTimestamp(),
      note: "extra",
    }));
  });

  test("Assigned Shipper can atomically complete after CUSTOMER_CONFIRMED", async () => {
    await assertSucceeds(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_CONFIRMED), {
      status: "DELIVERED",
      deliveryHandoffStatus: "COMPLETED",
      deliveryCompletedAt: serverTimestamp(),
      deliveredAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
      statusHistory: [
        ...history("DELIVERING", "SHIPPER", ASSIGNED_SHIPPER_ID),
        {
          status: "DELIVERED",
          actorRole: "SHIPPER",
          actorId: ASSIGNED_SHIPPER_ID,
          note: "Completed",
          createdAt: Timestamp.now(),
        },
      ],
    }));
  });

  test("Assigned Shipper cannot complete while LOCKED", async () => {
    await assertFails(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_DELIVERING_LOCKED), {
      status: "DELIVERED",
      deliveryHandoffStatus: "COMPLETED",
      deliveryCompletedAt: serverTimestamp(),
      deliveredAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
      statusHistory: history("DELIVERED", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
  });

  test("Assigned Shipper cannot complete while AWAITING_CUSTOMER", async () => {
    await assertFails(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_AWAITING), {
      status: "DELIVERED",
      deliveryHandoffStatus: "COMPLETED",
      deliveryCompletedAt: serverTimestamp(),
      deliveredAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
      statusHistory: history("DELIVERED", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
  });

  test("Other Shipper denied prepaid completion", async () => {
    await assertFails(updateDoc(orderDoc(OTHER_SHIPPER_ID, ORDER_VNPAY_CONFIRMED), {
      status: "DELIVERED",
      deliveryHandoffStatus: "COMPLETED",
      deliveryCompletedAt: serverTimestamp(),
      deliveredAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
      statusHistory: history("DELIVERED", "SHIPPER", OTHER_SHIPPER_ID),
    }));
  });

  test("Customer cannot set DELIVERED", async () => {
    await assertFails(updateDoc(orderDoc(OWNER_CUSTOMER_ID, ORDER_VNPAY_CONFIRMED), {
      status: "DELIVERED",
      deliveryHandoffStatus: "COMPLETED",
      deliveryCompletedAt: serverTimestamp(),
      deliveredAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
      statusHistory: history("DELIVERED", "CUSTOMER", OWNER_CUSTOMER_ID),
    }));
  });

  test("COD completion remains compatible", async () => {
    await assertSucceeds(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_COD_DELIVERING), {
      status: "DELIVERED",
      deliveredAt: serverTimestamp(),
      collectedByShipperId: ASSIGNED_SHIPPER_ID,
      collectedAmount: 22.5,
      cashCollected: true,
      updatedAt: serverTimestamp(),
      statusHistory: history("DELIVERED", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
  });

  test("Completion after CANCELLED denied", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), `orders/${ORDER_VNPAY_CONFIRMED}`), seededOrder({
        orderId: ORDER_VNPAY_CONFIRMED,
        orderCodeKey: ORDER_VNPAY_CONFIRMED,
        orderCode: `#${ORDER_VNPAY_CONFIRMED}`,
        status: "CANCELLED",
        shipperId: ASSIGNED_SHIPPER_ID,
        paymentMethod: "VNPAY",
        paymentStatus: "PAID",
        deliveryHandoffStatus: "CUSTOMER_CONFIRMED",
      }));
    });
    await assertFails(updateDoc(orderDoc(ASSIGNED_SHIPPER_ID, ORDER_VNPAY_CONFIRMED), {
      status: "DELIVERED",
      deliveryHandoffStatus: "COMPLETED",
      deliveryCompletedAt: serverTimestamp(),
      deliveredAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
      statusHistory: history("DELIVERED", "SHIPPER", ASSIGNED_SHIPPER_ID),
    }));
  });

  test("Existing assigned-Shipper tracking writes while DELIVERING still pass", async () => {
    await assertSucceeds(setDoc(trackingDoc(ASSIGNED_SHIPPER_ID), trackingPayload()));
  });

  test("Tracking write after DELIVERED still fails", async () => {
    await assertFails(setDoc(trackingDoc(ASSIGNED_SHIPPER_ID, ORDER_DELIVERED), trackingPayload()));
  });

  test("Owning-Customer tracking read still passes", async () => {
    await assertSucceeds(getDoc(trackingDoc(OWNER_CUSTOMER_ID)));
  });

  test("Other-Customer tracking read still fails", async () => {
    await assertFails(getDoc(trackingDoc(OTHER_CUSTOMER_ID)));
  });

  test("Tracking delete is denied", async () => {
    await assertFails(deleteDoc(trackingDoc(ASSIGNED_SHIPPER_ID)));
  });

  test("Customer cannot read paymentAttempts", async () => {
    await assertFails(getDoc(paymentAttemptDoc(OWNER_CUSTOMER_ID)));
  });

  test("Customer cannot write paymentAttempts", async () => {
    await assertFails(setDoc(paymentAttemptDoc(OWNER_CUSTOMER_ID), {
      status: "PAID",
    }));
  });

  test("Staff cannot access paymentAttempts", async () => {
    await assertFails(getDoc(paymentAttemptDoc(STAFF_ID)));
  });

  test("Shipper cannot access paymentAttempts", async () => {
    await assertFails(getDoc(paymentAttemptDoc(ASSIGNED_SHIPPER_ID)));
  });

  test("Admin client cannot access paymentAttempts", async () => {
    await assertFails(getDoc(paymentAttemptDoc(ADMIN_ID)));
  });

  test("fixture remains isolated from production Firebase", () => {
    assert.match(PROJECT_ID, /^demo-/);
  });
});
