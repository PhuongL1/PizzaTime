# PizzaTime Final QA

## Pre-Flight

- Confirm the repo has no staged secrets.
- Confirm `.idea/` is not staged.
- Confirm `serviceAccountKey.json` is not present.
- Confirm `node_modules/` and `functions/node_modules/` are not committed.
- Confirm Android builds pass.
- Confirm Firestore rules are deployed and active.
- Confirm no-Blaze demo mode is enabled with Storage upload and Cloud Functions disabled.

## Demo Accounts

- `customer@pizzatime.com` / `123456`
- `staff@pizzatime.com` / `123456`
- `kitchen@pizzatime.com` / `123456`
- `shipper@pizzatime.com` / `123456`
- `admin@pizzatime.com` / `123456`

## Customer QA

- Splash shows on launch.
- Logged-in customer routes to Customer Home.
- Back from home does not return to Login.
- Home loads Firestore products.
- Menu loads Firestore products.
- Detail shows product data and image URL when available.
- Favorites loads and updates correctly.
- Cart opens from product screens.
- Checkout creates an order in Firestore.
- Tracking updates in realtime.
- History shows customer orders.
- Profile loads Firestore profile data.
- Logout returns to guest/login flow.

## Staff QA

- Staff routes to Staff Dashboard.
- Pending orders appear in realtime.
- Staff can open order detail.
- Staff can confirm a pending order.
- Staff can cancel a pending or confirmed order if the UI exposes it.
- Staff logout returns to guest/login flow.

## Kitchen QA

- Kitchen routes to Kitchen Board.
- Confirmed orders appear in realtime.
- Kitchen can move orders through:
  - `CONFIRMED`
  - `PREPARING`
  - `BAKING`
  - `READY`
- Kitchen logout returns to guest/login flow.

## Shipper QA

- Shipper routes to Shipper Dashboard.
- Ready orders appear in realtime.
- Shipper can move orders through:
  - `READY`
  - `ASSIGNED_TO_SHIPPER`
  - `DELIVERING`
  - `DELIVERED`
- Customer tracking reflects delivery progress.
- Shipper logout returns to guest/login flow.

## Admin QA

- Admin routes to Admin Dashboard.
- Manage Menu loads products from Firestore.
- Product add, edit, and availability toggle still work with Firestore.
- Product image upload shows: "Image upload is temporarily disabled. Firebase Storage is not set up yet."
- Manage Promo loads and edits promo data.
- Manage Staff loads staff records.
- Manage Staff active toggle still works with Firestore.
- Staff create flow shows: "Staff account creation is temporarily disabled. Cloud Functions are not deployed yet."
- Reports load successfully.
- Admin logout returns to guest/login flow.

## Firebase Data Checks

- `users/{uid}` has the expected role and `active = true`.
- `orders/{orderId}` has status history entries.
- Existing `products/{id}.imageUrl` values display when valid; blank or invalid URLs use fallback drawables.
- Firestore rules still block invalid role transitions.
- Storage upload is disabled in no-Blaze demo mode.

## Required Deploy Commands

Run manually when rules or functions change:

```powershell
firebase deploy --only firestore:rules --project pizzatime-de04c
firebase deploy --only storage:rules --project pizzatime-de04c
firebase deploy --only functions --project pizzatime-de04c
```

## Known Risks

- Main order flow works with Firebase Auth and Firestore.
- Android FCM token registration remains enabled, but server-side notifications require deployed Functions.
- Staff account creation is disabled until the callable Function is deployed.
- Payment remains demo-only without a real gateway.
- Image upload is disabled until Firebase Storage is set up; image rendering still supports valid existing URLs and fallback drawables.
