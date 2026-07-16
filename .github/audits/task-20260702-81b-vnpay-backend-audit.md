# Task 81B Audit - Provider-Neutral Demo Payment Backend

Updated: 2026-07-16
Branch: feature/81-vnpay-handoff
Base commit inspected: cf6584f

## Scope

Task 81B now implements a provider-neutral Demo Payment backend instead of an active VNPay flow.

Preserved reusable work:

- isolated `payment-backend/` structure
- strict environment validation
- Firebase Admin initialization
- Firebase ID-token verification
- Customer ownership checks
- trusted server-side amount validation
- `paymentAttempts` repository
- idempotent create-payment flow
- Firestore transaction handling
- safe logging
- emulator integration test harness

Removed as active behavior:

- VNPay HMAC signing
- VNPay IPN confirmation
- VNPay Return URL flow
- mandatory `VNP_TMN_CODE`
- mandatory `VNP_HASH_SECRET`
- any claim that a real VNPay Sandbox payment is supported today

## Resume and repository state

- Branch verified: `feature/81-vnpay-handoff`
- Expected base commit verified: `cf6584f`
- CodeGraph index exists
- Node runtime on this machine: `v24.16.0`
- npm runtime on this machine: `11.13.0`

## Canonical order collection and owner field

- Canonical order path: `orders/{orderId}`
- Canonical owner field: `customerId`
- Canonical display reference fields:
  - `orderId`
  - `orderCodeKey`
  - `orderCode`

## Canonical payment fields

Task 81A established these order payment fields:

- `paymentMethod`
- `paymentStatus`
- `paymentProvider`
- `paymentAttemptId`
- `paymentReference`
- `paidAt`
- `providerTransactionId`
- `deliveryHandoffStatus`

Task 81B preserves these fields and updates only the payment-specific subset from the backend.

## Canonical order-total fields and pricing architecture

Observed order total fields remain:

- `itemsSubtotal`
- `subtotal`
- `deliveryFee`
- `discountAmount`
- `discount`
- `promoCode`
- `finalTotal`
- `total`

Observed current production pricing architecture remains partially client-derived:

- size multipliers and topping pricing still exist in Android logic
- product documents do not provide a full authoritative per-option VND price table
- Firestore Rules still do not derive totals from product and promo state

Safe selected strategy for Task 81B:

- only accept orders carrying `pricingSnapshotVnd`
- require integer VND values
- require snapshot reconciliation
- reject mismatched or legacy decimal orders

## Trusted amount strategy

The backend now trusts only this server-validated contract:

1. Read order by `orderId`.
2. Verify `customerId == verified Firebase uid`.
3. Verify `paymentMethod == DEMO`.
4. Verify order is payable and not already paid.
5. Read `pricingSnapshotVnd`.
6. Verify:
   - `currency == VND`
   - subtotal, discount, delivery fee, and total are integers
   - `itemsSubtotalVnd - discountVnd + deliveryFeeVnd == totalVnd`
   - stored `total` and `finalTotal` match `totalVnd` when present
7. Reject the create request if any check fails.

Result:

- legacy client-derived decimal totals are still untrusted
- real-provider settlement remains blocked until PizzaTime has a complete authoritative pricing source
- the Demo provider is still safe because it never represents a real-money transaction

## Payment domain update

Safest migration chosen:

- keep `VNPAY` in the payment domain for future real integration
- add `DEMO` as the active simulated prepaid method
- treat `DEMO` and `VNPAY` equivalently in the Task 81A prepaid policy

Meaning:

- staff confirmation still requires `PAID` for prepaid orders
- shipper start and prepaid handoff actions still require `PAID`
- customer receipt confirmation still applies to prepaid delivery
- COD behavior remains unchanged

## Create endpoint contract

Protected endpoint:

- `POST /api/v1/payments/create`

Headers:

- `Authorization: Bearer <Firebase ID token>`

Body:

```json
{
  "orderId": "de-3001",
  "requestId": "req-3001-abcdef"
}
```

The backend ignores extra client fields such as `amount` or `customerId`.

Success response:

```json
{
  "paymentAttemptId": "PT...",
  "paymentReference": "PT...",
  "paymentPageUrl": "https://<public-host>/demo/pay/<token>",
  "qrPayload": "https://<public-host>/demo/pay/<token>",
  "amountVnd": 123000,
  "expiresAt": "2026-07-16T08:45:00.000Z"
}
```

## Demo payment page contract

Public page:

- `GET /demo/pay/:token`

Behavior:

- renders English-only safe HTML
- shows order reference, amount, payment reference, and the testing warning
- includes `Confirm Demo Payment` and `Cancel Payment` buttons
- never changes payment state on `GET`
- sends `cache-control: no-store`
- escapes all interpolated values

## Confirm and cancel contracts

Confirm endpoint:

- `POST /demo/pay/:token/confirm`

Checks:

- token format is valid
- token hash matches one stored attempt
- attempt exists
- attempt is still `PENDING`
- token is not expired
- order exists
- order owns that attempt
- attempt is still the active order attempt
- trusted amount still matches
- order is not already paid

Atomic success update:

- `paymentAttempts/{attemptId}`
  - `status = PAID`
  - `provider = DEMO`
  - `confirmedAt = server timestamp`
  - `tokenConsumedAt = server timestamp`
  - `updatedAt = server timestamp`
- `orders/{orderId}`
  - `paymentStatus = PAID`
  - `paymentProvider = DEMO`
  - `paymentAttemptId = attemptId`
  - `paymentReference = reference`
  - `paidAt = server timestamp`

Cancel endpoint:

- `POST /demo/pay/:token/cancel`

Checks:

- token format is valid
- attempt exists
- attempt is still `PENDING`
- attempt is still current for the order
- order is not already paid

Atomic cancel update:

- `paymentAttempts/{attemptId}`
  - `status = FAILED`
  - `failureCode = CUSTOMER_CANCELLED`
  - `tokenConsumedAt = server timestamp`
  - `updatedAt = server timestamp`
- `orders/{orderId}`
  - `paymentStatus = FAILED` only if this attempt is still current

## Expiration and single-use token strategy

Active design:

- token entropy comes from `randomBytes(32)`
- raw token is derived from:
  - random `paymentTokenSalt`
  - `attemptId`
  - backend-only `DEMO_PAYMENT_TOKEN_SECRET`
- only `paymentTokenHash` and `paymentTokenSalt` are stored
- raw token is never persisted
- token is single-use because confirm/cancel consume it transactionally

Expiration:

- default session window: 15 minutes
- expired attempts are marked `EXPIRED` when a state-changing POST reaches them
- order `paymentStatus` becomes `EXPIRED` only if the expired attempt is still the active attempt

## Idempotency strategy

Create-payment idempotency identity:

- verified uid
- `orderId`
- `requestId`

Implementation:

- deterministic `paymentAttemptId`
- deterministic `requestIdHash`
- Firestore transaction
- same `requestId` returns the same attempt and the same payment page URL while pending and unexpired

Repeated confirm behavior:

- if already confirmed by the same attempt, return a safe already-confirmed page
- do not rewrite `paidAt`
- do not create duplicate transitions

Repeated cancel behavior:

- if already paid, never overwrite `PAID`
- if already failed or expired, return a safe non-destructive page

## paymentAttempts schema

Backend-only collection:

- `paymentAttempts/{paymentAttemptId}`

Current stored fields:

- `schemaVersion`
- `provider`
- `status`
- `orderId`
- `customerId`
- `transactionRef`
- `requestIdHash`
- `amountVnd`
- `providerAmount`
- `currency`
- `paymentTokenHash`
- `paymentTokenSalt`
- `createdAt`
- `expiresAt`
- `updatedAt`
- `confirmedAt`
- `failureCode`
- `tokenConsumedAt`

Not stored:

- Firebase ID token
- Authorization header
- raw demo payment token
- full payment URL
- service-account contents
- customer address
- phone
- order items

## Firestore Rules result

Root `firestore.rules` changed so prepaid initialization and prepaid-delivery rules now treat both:

- `DEMO`
- `VNPAY`

as prepaid payment methods.

The default deny rule still makes `paymentAttempts` backend-only.

Explicit result:

- Customer cannot read `paymentAttempts`
- Customer cannot write `paymentAttempts`
- Staff cannot access `paymentAttempts`
- Shipper cannot access `paymentAttempts`
- Admin client cannot access `paymentAttempts`
- client `PAID` writes remain denied

## Local development and tunnel strategy

Local backend:

- run on `http://localhost:8080`

Optional public access for QR or multi-device QA:

- `cloudflared tunnel --url http://localhost:8080`

Quick Tunnel is suitable for:

- demo page testing from another device
- QR scan testing

Quick Tunnel is not:

- stable production hosting
- a real payment gateway integration

## External setup still required

- `PUBLIC_BASE_URL`
- `DEMO_PAYMENT_TOKEN_SECRET`
- local `GOOGLE_APPLICATION_CREDENTIALS` when using real Firestore outside the emulator
- optional Cloudflare Tunnel if the demo page must be opened from another device

Not required for the active provider:

- `VNP_TMN_CODE`
- `VNP_HASH_SECRET`
- VNPay Sandbox merchant setup

## Files expected to change

- `.github/STATE.md`
- `.github/audits/task-20260702-81b-vnpay-backend-audit.md`
- `payment-backend/**`
- `firestore.rules`
- `payment-backend/firestore.rules`
- `firebase-rules-tests/test/firestore-tracking.rules.test.mjs`
- Task 81A payment-domain Kotlin files and tests

## Security boundaries

- this is a simulated payment provider
- no real money is transferred
- Android cannot mark payment as `PAID`
- only the backend confirm action can confirm the demo attempt
- cancel never overwrites `PAID`
- `paymentAttempts` is backend-only
- the backend remains ready for a future real payment-provider adapter
