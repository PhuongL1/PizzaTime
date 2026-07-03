# PizzaTime Final QA

## Pre-Flight

- Confirm the repo has no staged secrets.
- Confirm `.idea/` is not staged.
- Confirm `serviceAccountKey.json` is not present.
- Confirm `node_modules/` and `functions/node_modules/` are not committed.
- Confirm Android and Functions builds pass.

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
- Product image upload stores a download URL.
- Manage Promo loads and edits promo data.
- Manage Staff loads staff records.
- Staff create flow works through Cloud Functions.
- Reports load successfully.
- Admin logout returns to guest/login flow.

## Firebase Data Checks

- `users/{uid}` has the expected role and `active = true`.
- `orders/{orderId}` has status history entries.
- `products/{id}.imageUrl` is present after upload.
- Firestore rules still block invalid role transitions.
- Storage rules still require authenticated access.

## Required Deploy Commands

Run manually when rules or functions change:

```powershell
firebase deploy --only firestore:rules --project pizzatime-de04c
firebase deploy --only storage:rules --project pizzatime-de04c
firebase deploy --only functions --project pizzatime-de04c
```

## Known Risks

- Notifications require device permission and deployed Functions.
- Staff account creation requires the deployed callable Function.
- Payment remains demo-only without a real gateway.
- Image rendering depends on a valid Storage URL and network access.
