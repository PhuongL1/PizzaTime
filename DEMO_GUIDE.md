# PizzaTime Demo Guide

## Overview

PizzaTime is an Android Kotlin app built with XML layouts, Fragments, ViewBinding, Firebase Auth, Firestore, Storage-ready image code, FCM, and Cloud Functions-ready admin code.

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
- Firestore is deployed and active for users, categories, products, promo codes, and orders.
- Storage upload is disabled in no-Blaze demo mode until the Firebase Storage bucket is set up.
- FCM token registration is saved to `users/{uid}.fcmTokens`.
- Cloud Function staff creation and server-side FCM notifications are disabled until Blaze/Billing is fixed and Functions are deployed.

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
   - image upload shows the no-Blaze disabled message
   - toggle availability
4. Manage Promo:
   - edit promo fields
   - toggle active state
5. Manage Staff:
   - view users
   - toggle active state
   - staff creation shows the no-Blaze disabled message
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
- Main order flow works with Firebase Auth and deployed Firestore.
- Android FCM token registration and notification permission behavior remain enabled, but server-side notification delivery requires deployed Cloud Functions.
- Admin-created staff accounts are temporarily disabled until Cloud Functions are deployed.
- Product image upload is temporarily disabled until Firebase Storage is set up; existing `imageUrl` values still display, with fallback drawables for blank or invalid URLs.
- Some secondary screens still use fallback demo data if Firestore is unavailable.

## Submission Notes

- Do not commit `.idea` files, temp files, `serviceAccountKey.json`, or `node_modules`.
- Keep Firebase rules and functions deployment manual.
- Use the QA checklist in `FINAL_QA.md` before presenting the demo.
