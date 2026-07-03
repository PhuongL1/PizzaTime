# PizzaTime Demo Guide

## Project Overview

PizzaTime is an Android Kotlin pizza ordering app built with XML layouts, Fragments, and ViewBinding. The app uses Firebase Auth for role-based login and Cloud Firestore for product, promo, order, user, and admin data.

Core demo flows include customer ordering, promo application, realtime order tracking, realtime staff/kitchen/shipper dashboards, customer order history, customer profile, and admin management screens.

## Firebase Setup Summary

- Firebase Auth is used for sign-in.
- Firestore stores app data for users, categories, products, promo codes, and orders.
- Firestore security rules are expected to be deployed before demo.
- The Android app expects Firebase configuration through the checked-in app configuration files already present in the project.
- Do not commit service account keys or private Firebase admin credentials.

## Test Accounts

Use these seeded Firebase Auth accounts:

| Role | Email | Password |
| --- | --- | --- |
| Customer | customer@pizzatime.com | 123456 |
| Staff | staff@pizzatime.com | 123456 |
| Kitchen | kitchen@pizzatime.com | 123456 |
| Shipper | shipper@pizzatime.com | 123456 |
| Admin | admin@pizzatime.com | 123456 |

Each account should have a matching `users/{uid}` Firestore document with `active = true` and the expected role.

## Demo Flow

1. Login as `customer@pizzatime.com`.
2. Browse products loaded from Firestore.
3. Add a product to the cart.
4. Apply an active promo code.
5. Checkout and create a Firestore order.
6. Verify the Firestore order includes `customerId`, `deliveryAddress`, `promoCode`, `discount`, `status = PENDING`, and a `statusHistory` item for `PENDING`.
7. Open realtime customer order tracking.
8. Login as `staff@pizzatime.com`; the Staff Dashboard should update automatically when the pending order appears.
9. Confirm the pending order from Staff Dashboard.
10. Login as `kitchen@pizzatime.com`; the Kitchen Board should update automatically when the order becomes `CONFIRMED`.
11. Move the order through kitchen statuses to `READY`.
12. Login as `shipper@pizzatime.com`; the Shipper Dashboard should update automatically when the order becomes `READY`.
13. Move the order to `DELIVERED`.
14. Return to the customer tracking screen and verify realtime status updates throughout the lifecycle.
15. Open customer order history and order detail.
16. Login as `admin@pizzatime.com` and open dashboard/reports.
17. Edit product, promo, and staff status from admin screens.

## Firestore Collections

- `users`
- `categories`
- `products`
- `promoCodes`
- `orders`

## Order Status Lifecycle

Expected order lifecycle:

| Status | Responsible role |
| --- | --- |
| `PENDING` | `CUSTOMER` |
| `CONFIRMED` | `STAFF` |
| `PREPARING` | `KITCHEN` |
| `BAKING` | `KITCHEN` |
| `READY` | `KITCHEN` |
| `ASSIGNED_TO_SHIPPER` | `SHIPPER` |
| `DELIVERING` | `SHIPPER` |
| `DELIVERED` | `SHIPPER` |

Role handoff:

- Customer creates `PENDING` orders.
- Staff Dashboard listens realtime and confirms `PENDING -> CONFIRMED`.
- Kitchen Board listens realtime and updates `CONFIRMED -> PREPARING -> BAKING -> READY`.
- Shipper Dashboard listens realtime and updates `READY -> ASSIGNED_TO_SHIPPER -> DELIVERING -> DELIVERED`.
- Customer Order Tracking listens realtime and reflects status changes throughout the full lifecycle.

## Order Status History

Each order stores the current status in the `status` field. Each order also stores a `statusHistory` array as an audit trail for major lifecycle updates.

Every status transition appends a history item so the demo can show who changed the order and when. The app currently records history in Firestore; it does not redesign the customer tracking UI to display the full audit list.

Status history item shape:

```json
{
  "status": "PENDING",
  "actorRole": "CUSTOMER",
  "actorId": "firebase-auth-uid",
  "note": "Order placed",
  "createdAt": "timestamp"
}
```

Recorded history behavior:

- Checkout creates the initial `PENDING` item with `actorRole = CUSTOMER`.
- Staff confirmation appends `CONFIRMED` with `actorRole = STAFF`.
- Kitchen updates append `PREPARING`, `BAKING`, and `READY` with `actorRole = KITCHEN`.
- Shipper updates append `ASSIGNED_TO_SHIPPER`, `DELIVERING`, and `DELIVERED` with `actorRole = SHIPPER`.

## Firestore Rules Note

Firestore security rules restrict order status updates by role:

- `STAFF`: `PENDING -> CONFIRMED`
- `KITCHEN`: `CONFIRMED -> PREPARING -> BAKING -> READY`
- `SHIPPER`: `READY -> ASSIGNED_TO_SHIPPER -> DELIVERING -> DELIVERED`

The rules were updated so `statusHistory` can be changed only during valid role status transitions. After pulling the latest rules, deploy them manually:

```powershell
firebase deploy --only firestore:rules --project pizzatime-de04c
```

## Known Limitations

- No Firebase Storage image upload yet.
- No real payment gateway.
- No push notifications or FCM yet.
- Staff account creation from the app is not implemented.
- Some UI screens still use fallback fake data if Firestore fails.
- Realtime is implemented for customer order tracking and the active Staff, Kitchen, and Shipper order dashboards. Some secondary list screens may still use one-time reads.

## Manual QA Checklist

- Customer login succeeds and rejects inactive users.
- Customer product list loads from Firestore.
- Customer can add a product to cart.
- Customer can apply a valid active promo code in checkout.
- Checkout creates an order document in `orders`.
- Customer realtime tracking updates as staff roles change status.
- Staff can confirm a pending order.
- Kitchen can move an order to ready.
- Shipper can move an order to delivered.
- Customer order history and detail show Firestore orders.
- Customer account loads and updates Firestore profile fields.
- Admin dashboard and reports load Firestore data.
- Admin can edit product fields and toggle availability.
- Admin can edit promo fields and toggle active state.
- Admin can view and update staff status.
- Firestore security rules block unauthenticated access.
- Staff, kitchen, shipper, and admin logins route to the correct screens.

## Final Realtime QA Checklist

1. Login as customer and create an order.
2. Verify Staff Dashboard updates automatically with the new `PENDING` order.
3. Staff confirms the order.
4. Verify Kitchen Board updates automatically with the `CONFIRMED` order.
5. Kitchen updates the order to `READY`.
6. Verify Shipper Dashboard updates automatically with the `READY` order.
7. Shipper updates the order to `DELIVERED`.
8. Verify Customer Order Tracking updates realtime throughout the lifecycle.
9. Verify Admin dashboard/reports still load.
10. Verify Firestore security rules do not block valid role actions.

## Status History QA Checklist

1. Deploy the latest Firestore Rules.
2. Login as `customer@pizzatime.com / 123456`.
3. Create a new order from Checkout.
4. Open Firebase Console -> Firestore -> `orders/{orderId}`.
5. Verify `status = PENDING`.
6. Verify `statusHistory` contains a `PENDING` item with `actorRole = CUSTOMER`.
7. Login as `staff@pizzatime.com / 123456`.
8. Confirm the order.
9. Verify `status = CONFIRMED`.
10. Verify `statusHistory` contains a `CONFIRMED` item with `actorRole = STAFF`.
11. Login as `kitchen@pizzatime.com / 123456`.
12. Update `CONFIRMED -> PREPARING`.
13. Update `PREPARING -> BAKING`.
14. Update `BAKING -> READY`.
15. Verify `statusHistory` contains `PREPARING`, `BAKING`, and `READY` items with `actorRole = KITCHEN`.
16. Login as `shipper@pizzatime.com / 123456`.
17. Update `READY -> ASSIGNED_TO_SHIPPER`.
18. Update `ASSIGNED_TO_SHIPPER -> DELIVERING`.
19. Update `DELIVERING -> DELIVERED`.
20. Verify `statusHistory` contains `ASSIGNED_TO_SHIPPER`, `DELIVERING`, and `DELIVERED` items with `actorRole = SHIPPER`.
21. Keep Customer Tracking open and verify realtime UI updates.
22. Verify invalid role transitions are blocked after rules deploy.

## Final Full Demo Flow

1. Customer login.
2. Customer edits `deliveryAddress` in Account.
3. Customer opens Pizza Menu.
4. Customer adds product to cart.
5. Customer applies promo code.
6. Customer places order.
7. Firestore order is created with `customerId`, `deliveryAddress`, `promoCode`, `discount`, `status = PENDING`, and `statusHistory` with `PENDING`.
8. Staff Dashboard receives the order realtime.
9. Staff confirms the order.
10. Kitchen Board receives the order realtime.
11. Kitchen updates the order to `READY`.
12. Shipper Dashboard receives the order realtime.
13. Shipper updates the order to `DELIVERED`.
14. Customer Tracking updates realtime.
15. Customer Order History/Detail shows the order.
16. Admin Dashboard/Reports reflect the order.
17. Admin can manage products, promos, and staff.

## Build Check

Run before submission:

```powershell
.\gradlew.bat build
```

Expected result: `BUILD SUCCESSFUL`.
