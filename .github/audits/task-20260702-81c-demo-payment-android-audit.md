# Task 81C Audit - Automated Demo QR Payment Android Flow

Updated: 2026-07-16
Branch: feature/81-vnpay-handoff
Base commit inspected: d7962c7

## Resume state

- Task 81A remains the payment and handoff foundation.
- Task 81B remains the active provider-neutral Demo payment backend.
- Branch and base commit were re-verified before edits.
- Protected local files and untracked workspace items remain untouched.

## Implemented Checkout creation sequence

Checkout now has two explicit Customer-only payment options:

1. `Cash on Delivery`
2. `Demo QR Payment`

Implemented flow in `CheckoutFragment`:

1. Load cart, promo, delivery estimate, and server-compatible order pricing.
2. Keep COD selected by default unless restored state says otherwise.
3. Disable Demo selection safely when `BuildConfig.PAYMENT_BACKEND_URL` is empty or invalid.
4. For COD:
   - create one order with `paymentMethod = COD`
   - keep `paymentStatus = NOT_REQUIRED`
   - keep `deliveryHandoffStatus = NOT_REQUIRED`
   - clear cart immediately
   - navigate directly to `OrderSuccessFragment`
5. For Demo:
   - create one order with `paymentMethod = DEMO`
   - set `paymentStatus = PENDING`
   - set `paymentProvider = DEMO`
   - set `deliveryHandoffStatus = LOCKED`
   - do not clear the cart
   - do not navigate to success
   - persist one pending payment state and open `DemoPaymentFragment`

## Cart-clearing timing

Cart clearing is now split safely:

- COD still clears immediately after order creation.
- Demo orders keep the cart during `PENDING`, `FAILED`, and `EXPIRED`.
- Demo cart clearing happens only after Firestore confirms `paymentStatus == PAID`.
- The exactly-once guard is persisted per authenticated customer through `DemoPaymentPendingStore`.

## Order Success navigation

The only success destination remains `OrderSuccessFragment(orderId)`.

Implemented exactly-once path:

1. Firestore order listener observes `paymentStatus == PAID`.
2. The app validates current authenticated user and active payment attempt.
3. `DemoPaymentPendingStore.markSuccessNavigationPending(...)` persists the one-time success marker.
4. Cart clears once.
5. `openOrderSuccess(orderId, addToBackStack = false)` runs once when lifecycle is safe.

Deep links do not confirm payment and do not bypass this Firestore-based path.

## Android network stack

The app still does not add Retrofit or OkHttp.

Implemented Android backend client:

- `DemoPaymentBackendRepository`
- `HttpURLConnection`
- coroutine-based request execution
- Firebase ID token attachment
- one forced token refresh on `401`
- typed error mapping
- no ID token logging
- no full payment URL or QR payload logging

## Backend endpoint contract in use

Android uses Task 81B’s active endpoint:

- `POST /api/v1/payments/create`

Request body contains only:

```json
{
  "orderId": "order-id",
  "requestId": "uuid"
}
```

Trusted response fields consumed by Android:

- `paymentAttemptId`
- `paymentReference`
- `amountVnd`
- `expiresAt`
- `paymentPageUrl`
- `qrPayload`

Validation enforced on Android:

- HTTPS required for public hosts
- debug HTTP allowed only for `10.0.2.2`, `127.0.0.1`, and `localhost`
- `qrPayload` must equal `paymentPageUrl`
- origin must match configured backend origin
- token path must match `/demo/pay/<token>`

## Backend URL configuration strategy

Implemented configuration:

- `local.properties`
  - `PIZZATIME_PAYMENT_BACKEND_URL=https://example.trycloudflare.com/`
- `app/build.gradle.kts`
  - exposes the value as `BuildConfig.PAYMENT_BACKEND_URL`

Behavior:

- empty or invalid config does not break builds
- COD remains usable
- Demo QR Payment stays visible but disabled
- Checkout shows: `Demo payment service is not configured.`
- debug-only cleartext is limited to local emulator hosts through `app/src/debug/res/xml/debug_network_security_config.xml`

## RequestId persistence strategy

Implemented local persistence in `DemoPaymentPendingStore`:

- one active state per authenticated customer
- fields:
  - `orderId`
  - `requestId`
  - `paymentAttemptId`
  - `paymentReference`
  - `expiresAtIso`
  - `amountVnd`

Behavior:

- `ensureActiveState(...)` reuses the same `requestId` for the same pending order
- `createNewAttemptState(...)` creates a fresh `requestId` for intentional retry
- rotation and process recreation reuse the stored `requestId`
- the persisted expiry is now canonical UTC ISO, not `Date.toString()`

## Payment screen state machine

Implemented `DemoPaymentFragment` states:

- creating payment session
- waiting for payment
- payment confirmed
- payment cancelled
- payment session expired
- payment refunded
- payment service unavailable
- unsupported payment state

Visible screen content:

- title: `Demo QR Payment`
- order reference
- trusted amount
- payment reference
- expiration
- QR code
- `Open Payment Page`
- `Refresh Payment Status`
- `Create New Payment`
- `Back to Orders`
- disclaimer:
  - `For testing purposes only.`
  - `No real money will be transferred.`

## QR rendering strategy

Implemented QR generation:

- ZXing core only
- generated from backend `qrPayload`
- off the main thread
- bounded `720x720`
- if generation fails:
  - keep `Open Payment Page` available
  - show `Unable to display the QR code.`

## Browser and payment page strategy

Implemented:

- prefer AndroidX Browser Custom Tabs
- fallback to `ACTION_VIEW`
- no WebView
- no arbitrary URI launch
- no browser available message:
  - `No browser is available to open the payment page.`

## Firestore payment listener strategy

Implemented:

- one `ListenerRegistration` per visible `DemoPaymentFragment`
- listener removed on `onDestroyView`
- old listeners removed before replacement
- snapshots are accepted only if `customerId` matches the current authenticated user
- Firestore order document remains the only payment source of truth

Mapped states:

- `PENDING` -> Waiting for payment
- `PAID` -> Payment confirmed
- `FAILED` -> Payment cancelled
- `EXPIRED` -> Payment session expired
- `REFUNDED` -> Payment refunded

## Continue Payment and retry behavior

Implemented order-detail behavior:

- Demo orders with `PENDING` show `Continue Payment`
- Demo orders with `FAILED` or `EXPIRED` show `Create New Payment`
- `PAID` hides payment actions

Retry behavior:

- same order is reused
- backend receives a new `requestId`
- new attempt/reference/QR/session is created
- no second order is created

## Notification dedupe strategy

Implemented payment notification support in the existing notification system:

- new type: `CUSTOMER_PAYMENT_RECEIVED`
- new dedupe key:
  - `payment:<orderId>:paid:<paymentAttemptId>`
- one customer inbox event only
- one unread increment only
- no second subsystem added

## Deep-link strategy

Implemented Customer-only deep link:

- `pizzatime://payment-result`

Accepted server-generated query parameters:

- `orderId`
- `paymentAttemptId`

Ignored and untrusted:

- `status`
- `amount`
- unknown parameters

Routing behavior:

- handled only in Customer edition
- captured in `MainActivity`
- restored through `DemoPaymentDeepLinkCoordinator`
- navigates back to `DemoPaymentFragment`
- Firestore still decides whether the order is really `PAID`

## Process-death recovery

Implemented recovery strategy:

- pending order/session state persists in `SharedPreferences`
- success navigation marker persists separately
- when the app resumes:
  - same customer restores the active payment flow
  - same `requestId` is reused for interrupted session creation
  - verified `PAID` completes success once
- account switch isolates state by user key and avoids cross-account cart clearing

## Backend integration changes for 81C

Backend changes were limited to Android integration and idempotent session reconstruction:

- deterministic demo payment token derivation for stable repeated create-session URLs
- safe `APP_RETURN_DEEP_LINK_BASE` validation
- result pages now support `Return to PizzaTime`
- deep-link payload contains only:
  - `orderId`
  - `paymentAttemptId`
- no status, amount, or token is returned to Android

## Manual setup requirements

Local-only values still required for real tunnel QA:

- `payment-backend/.env`
  - `NODE_ENV=development`
  - `PAYMENT_PROVIDER=DEMO`
  - `DEMO_PAYMENT_ENABLED=true`
  - `DEMO_PAYMENT_TOKEN_SECRET=<local secret>`
  - `PUBLIC_BASE_URL=https://<active-host>`
  - `APP_RETURN_DEEP_LINK_BASE=pizzatime://payment-result`
- `local.properties`
  - `PIZZATIME_PAYMENT_BACKEND_URL=https://<active-host>/`
- optional `cloudflared tunnel --url http://localhost:8080`

None of those local values were committed.

## Files changed for Task 81C

- `.github/STATE.md`
- `.github/audits/task-20260702-81c-demo-payment-android-audit.md`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/debug/AndroidManifest.xml`
- `app/src/debug/res/xml/debug_network_security_config.xml`
- `app/src/customer/AndroidManifest.xml`
- `app/src/main/res/layout/fragment_checkout.xml`
- `app/src/main/res/layout/fragment_demo_payment.xml`
- `app/src/main/res/values/strings.xml`
- checkout, payment, order-detail, notification, and navigation Kotlin files
- Android unit tests for demo payment config/contract/pending-store and notification dedupe
- Task 81B backend adapter, token, return-link, and backend tests

## Honest QA boundary

Automated verification is complete for backend, rules, and Android builds.

Real end-to-end Demo payment QA is still blocked by local environment availability:

- no committed or present `payment-backend/.env`
- no attached Android device or emulator at audit time
- no active public HTTPS tunnel at audit time

That means Task 81C cannot honestly claim final `COMPLETE` from this worktree alone.
