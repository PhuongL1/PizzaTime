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
} from "firebase/firestore";

const PROJECT_ID = "demo-pizzatime-rules";
const OWNER_CUSTOMER_ID = "customer-owner";
const OTHER_CUSTOMER_ID = "customer-other";
const ASSIGNED_SHIPPER_ID = "shipper-assigned";
const OTHER_SHIPPER_ID = "shipper-other";
const STAFF_ID = "staff-user";
const DELIVERING_ORDER_ID = "order-delivering";
const PENDING_ORDER_ID = "order-pending";
const DELIVERED_ORDER_ID = "order-delivered";

let testEnvironment;

function trackingPath(orderId = DELIVERING_ORDER_ID) {
  return `orders/${orderId}/tracking/current`;
}

function trackingDocumentFor(userId, orderId = DELIVERING_ORDER_ID) {
  const firestore = testEnvironment.authenticatedContext(userId).firestore();
  return doc(firestore, trackingPath(orderId));
}

function validTrackingPayload(overrides = {}) {
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
    ];

    for (const [userId, role] of users) {
      await setDoc(doc(firestore, `users/${userId}`), {
        id: userId,
        role,
        active: true,
      });
    }

    await setDoc(doc(firestore, `orders/${DELIVERING_ORDER_ID}`), {
      customerId: OWNER_CUSTOMER_ID,
      shipperId: ASSIGNED_SHIPPER_ID,
      status: "DELIVERING",
    });
    await setDoc(doc(firestore, `orders/${PENDING_ORDER_ID}`), {
      customerId: OWNER_CUSTOMER_ID,
      shipperId: ASSIGNED_SHIPPER_ID,
      status: "PENDING",
    });
    await setDoc(doc(firestore, `orders/${DELIVERED_ORDER_ID}`), {
      customerId: OWNER_CUSTOMER_ID,
      shipperId: ASSIGNED_SHIPPER_ID,
      status: "DELIVERED",
    });
    await setDoc(doc(firestore, trackingPath()), {
      ...validTrackingPayload(),
      updatedAt: Timestamp.now(),
    });
  });
}

describe("orders/{orderId}/tracking/current Firestore rules", () => {
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

  test("owning Customer can read current tracking", async () => {
    await assertSucceeds(getDoc(trackingDocumentFor(OWNER_CUSTOMER_ID)));
  });

  test("another Customer cannot read current tracking", async () => {
    await assertFails(getDoc(trackingDocumentFor(OTHER_CUSTOMER_ID)));
  });

  test("assigned Shipper can read current tracking", async () => {
    await assertSucceeds(getDoc(trackingDocumentFor(ASSIGNED_SHIPPER_ID)));
  });

  test("another Shipper cannot read current tracking", async () => {
    await assertFails(getDoc(trackingDocumentFor(OTHER_SHIPPER_ID)));
  });

  test("Staff cannot read current tracking", async () => {
    await assertFails(getDoc(trackingDocumentFor(STAFF_ID)));
  });

  test("unauthenticated user cannot read current tracking", async () => {
    const firestore = testEnvironment.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(firestore, trackingPath())));
  });

  test("assigned Shipper can write while the parent order is DELIVERING", async () => {
    await assertSucceeds(
      setDoc(trackingDocumentFor(ASSIGNED_SHIPPER_ID), validTrackingPayload()),
    );
  });

  test("optional bearing and speed may be omitted", async () => {
    const payload = validTrackingPayload();
    delete payload.bearingDegrees;
    delete payload.speedMetersPerSecond;
    await assertSucceeds(setDoc(trackingDocumentFor(ASSIGNED_SHIPPER_ID), payload));
  });

  test("assigned Shipper cannot write before DELIVERING", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID, PENDING_ORDER_ID),
        validTrackingPayload(),
      ),
    );
  });

  test("assigned Shipper cannot write after DELIVERED", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID, DELIVERED_ORDER_ID),
        validTrackingPayload(),
      ),
    );
  });

  test("Customer cannot write current tracking", async () => {
    await assertFails(
      setDoc(trackingDocumentFor(OWNER_CUSTOMER_ID), validTrackingPayload()),
    );
  });

  test("Staff cannot write current tracking", async () => {
    await assertFails(setDoc(trackingDocumentFor(STAFF_ID), validTrackingPayload()));
  });

  test("another Shipper cannot write current tracking", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(OTHER_SHIPPER_ID),
        validTrackingPayload({ shipperId: OTHER_SHIPPER_ID }),
      ),
    );
  });

  test("extra tracking field is denied", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ customerPhone: "not-allowed" }),
      ),
    );
  });

  test("mismatched shipperId is denied", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ shipperId: OTHER_SHIPPER_ID }),
      ),
    );
  });

  test("invalid location type is denied", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ location: "10.7769,106.7009" }),
      ),
    );
  });

  test("out-of-bounds accuracy is denied", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ accuracyMeters: 100.1 }),
      ),
    );
  });

  test("stale recordedAt timestamp is denied", async () => {
    const elevenMinutesAgo = Timestamp.fromMillis(Date.now() - 11 * 60 * 1000);
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ recordedAt: elevenMinutesAgo }),
      ),
    );
  });

  test("future recordedAt timestamp is denied", async () => {
    const twoMinutesFromNow = Timestamp.fromMillis(Date.now() + 2 * 60 * 1000);
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ recordedAt: twoMinutesFromNow }),
      ),
    );
  });

  test("out-of-bounds optional bearing is denied", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ bearingDegrees: 360 }),
      ),
    );
  });

  test("out-of-bounds optional speed is denied", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ speedMetersPerSecond: 100.1 }),
      ),
    );
  });

  test("client timestamp in updatedAt is denied", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ updatedAt: Timestamp.now() }),
      ),
    );
  });

  test("unexpected schema version is denied", async () => {
    await assertFails(
      setDoc(
        trackingDocumentFor(ASSIGNED_SHIPPER_ID),
        validTrackingPayload({ schemaVersion: 2 }),
      ),
    );
  });

  test("tracking document delete is denied", async () => {
    await assertFails(deleteDoc(trackingDocumentFor(ASSIGNED_SHIPPER_ID)));
  });

  test("a tracking history document is denied", async () => {
    const firestore = testEnvironment.authenticatedContext(ASSIGNED_SHIPPER_ID).firestore();
    await assertFails(
      setDoc(
        doc(firestore, `orders/${DELIVERING_ORDER_ID}/tracking/history-1`),
        validTrackingPayload(),
      ),
    );
  });

  test("fixture remains isolated from production Firebase", () => {
    assert.match(PROJECT_ID, /^demo-/);
  });
});
