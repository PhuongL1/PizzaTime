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
6. Open realtime customer order tracking.
7. Login as `staff@pizzatime.com`; the Staff Dashboard should update automatically when the pending order appears.
8. Confirm the pending order from Staff Dashboard.
9. Login as `kitchen@pizzatime.com`; the Kitchen Board should update automatically when the order becomes `CONFIRMED`.
10. Move the order through kitchen statuses to `READY`.
11. Login as `shipper@pizzatime.com`; the Shipper Dashboard should update automatically when the order becomes `READY`.
12. Move the order to `DELIVERED`.
13. Return to the customer tracking screen and verify realtime status updates throughout the lifecycle.
14. Open customer order history and order detail.
15. Login as `admin@pizzatime.com` and open dashboard/reports.
16. Edit product, promo, and staff status from admin screens.

## Firestore Collections

- `users`
- `categories`
- `products`
- `promoCodes`
- `orders`

## Order Status Lifecycle

Expected order lifecycle:

1. `PENDING`
2. `CONFIRMED`
3. `PREPARING`
4. `BAKING`
5. `READY`
6. `ASSIGNED_TO_SHIPPER`
7. `DELIVERING`
8. `DELIVERED`

Role handoff:

- Customer creates `PENDING` orders.
- Staff Dashboard listens realtime and confirms `PENDING -> CONFIRMED`.
- Kitchen Board listens realtime and updates `CONFIRMED -> PREPARING -> BAKING -> READY`.
- Shipper Dashboard listens realtime and updates `READY -> ASSIGNED_TO_SHIPPER -> DELIVERING -> DELIVERED`.
- Customer Order Tracking listens realtime and reflects status changes throughout the full lifecycle.

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

## Build Check

Run before submission:

```powershell
.\gradlew.bat build
```

Expected result: `BUILD SUCCESSFUL`.
