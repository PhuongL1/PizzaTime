# PizzaTime Demo Guide

## Overview

PizzaTime is an Android Kotlin app built with XML layouts, Fragments, ViewBinding, Firebase Auth, Firestore, Storage, FCM, and Cloud Functions.

Main demo areas:

- Customer browsing, cart, checkout, tracking, history, favorites, profile
- Staff order confirmation and cancellation
- Kitchen order progression
- Shipper delivery progression
- Admin product, promo, staff, and report management

The app restores the authenticated session from Splash and routes by Firestore role.

## Demo Accounts

| Role | Email | Password |
| --- | --- | --- |
| Customer | `customer@pizzatime.com` | `123456` |
| Staff | `staff@pizzatime.com` | `123456` |
| Kitchen | `kitchen@pizzatime.com` | `123456` |
| Shipper | `shipper@pizzatime.com` | `123456` |
| Admin | `admin@pizzatime.com` | `123456` |

Expected role routing after login or app relaunch:

- CUSTOMER -> Customer Home
- STAFF -> Staff Dashboard
- KITCHEN -> Kitchen Board
- SHIPPER -> Shipper Dashboard
- ADMIN -> Admin Dashboard

## Firebase Setup

- Firebase Auth handles login.
- Firestore stores users, categories, products, promo codes, and orders.
- Storage stores product images and returns download URLs saved to `products/{id}.imageUrl`.
- FCM token registration is saved to `users/{uid}.fcmTokens`.
- Cloud Functions send order-status notifications and create staff accounts.

Firebase config in the repo:

- `firebase.json`
- `firestore.rules`
- `storage.rules`
- `functions/`

## Customer Demo Flow

1. Open the app and wait for Splash.
2. Login as `customer@pizzatime.com`.
3. Browse Home, Menu, Detail, Favorites, Profile, Tracking, and History.
4. Open a product and confirm the Firestore image URL displays when available.
5. Add a product to the cart.
6. Apply a promo code if one is active.
7. Checkout to create a Firestore order.
8. Verify the order enters `PENDING`.
9. Watch realtime tracking update as staff roles move the order forward.
10. Open order history and order detail.
11. Log out from profile or menu.

Customer screens used in the demo:

- Splash/Login
- Home
- Menu
- Detail
- Cart/Checkout
- Promo
- Tracking
- History
- Favorites
- Profile
- Logout

## Staff Demo Flow

1. Login as `staff@pizzatime.com`.
2. Open Staff Dashboard.
3. View the pending order.
4. Open order detail if needed.
5. Confirm the order.
6. Cancel a pending or confirmed order if the UI exposes the action.
7. Log out.

## Kitchen Demo Flow

1. Login as `kitchen@pizzatime.com`.
2. Open Kitchen Board.
3. Process orders through:
   - `CONFIRMED`
   - `PREPARING`
   - `BAKING`
   - `READY`
4. Open order detail if needed.
5. Log out.

## Shipper Demo Flow

1. Login as `shipper@pizzatime.com`.
2. Open Shipper Dashboard.
3. Process orders through:
   - `READY`
   - `ASSIGNED_TO_SHIPPER`
   - `DELIVERING`
   - `DELIVERED`
4. Verify delivery completion updates the customer tracking screen.
5. Log out.

## Admin Demo Flow

1. Login as `admin@pizzatime.com`.
2. Open Admin Dashboard.
3. Manage Menu:
   - edit products
   - upload product images
   - toggle availability
4. Manage Promo:
   - edit promo fields
   - toggle active state
5. Manage Staff:
   - view users
   - toggle active state
   - create staff/kitchen/shipper accounts through the Cloud Function flow
6. Open Reports.
7. Log out.

## Build Commands

Android:

```powershell
.\gradlew.bat clean
.\gradlew.bat build
```

Functions:

```powershell
cd functions
npm run build
cd ..
```

## Firebase Deploy Commands

Deploy Firestore rules:

```powershell
firebase deploy --only firestore:rules --project pizzatime-de04c
```

Deploy Storage rules:

```powershell
firebase deploy --only storage:rules --project pizzatime-de04c
```

Deploy Cloud Functions:

```powershell
firebase deploy --only functions --project pizzatime-de04c
```

## Known Limitations

- Payment is demo only if no real payment gateway is connected.
- Notification delivery depends on device notification permission and deployed Cloud Functions.
- Admin-created staff accounts require deployed Cloud Functions.
- Product images depend on a valid `Storage` download URL in Firestore.
- Some secondary screens still use fallback demo data if Firestore is unavailable.

## Submission Notes

- Do not commit `.idea` files, temp files, `serviceAccountKey.json`, or `node_modules`.
- Keep Firebase rules and functions deployment manual.
- Use the QA checklist in `FINAL_QA.md` before presenting the demo.
