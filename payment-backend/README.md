# PizzaTime Payment Backend

## Status

This backend now runs a provider-neutral Demo Payment flow.

- It is a simulated payment provider.
- No real money is transferred.
- No VNPay credential is required for the active provider.
- `NODE_ENV=production` with `DEMO_PAYMENT_ENABLED=true` is rejected at startup.

The backend still requires a trusted integer-VND pricing snapshot on the order. It does not trust the legacy client-derived decimal totals already present elsewhere in PizzaTime.

## Active Flow

- `POST /api/v1/payments/create`
  - verifies Firebase ID token
  - verifies Customer ownership
  - verifies `paymentMethod == DEMO`
  - verifies trusted amount from `pricingSnapshotVnd`
  - creates one idempotent `paymentAttempts/{attemptId}` record
  - returns `paymentAttemptId`, `paymentReference`, `amountVnd`, `expiresAt`, `paymentPageUrl`, and `qrPayload`
- `GET /demo/pay/:token`
  - renders the public Demo Payment page
  - never changes payment state
- `POST /demo/pay/:token/confirm`
  - confirms the demo payment transactionally
  - only the backend confirm action can mark the order `PAID`
- `POST /demo/pay/:token/cancel`
  - marks the active pending attempt failed
  - never overwrites `PAID`

## Environment

Create `payment-backend/.env` from `.env.example` and fill placeholders locally.

Required values:

- `NODE_ENV`
- `PORT`
- `FIREBASE_PROJECT_ID`
- `PAYMENT_PROVIDER=DEMO`
- `DEMO_PAYMENT_ENABLED=true`
- `DEMO_PAYMENT_TOKEN_SECRET`
- `PUBLIC_BASE_URL`
- `PAYMENT_SESSION_MINUTES`

Optional:

- `APP_RETURN_DEEP_LINK_BASE`

Notes:

- `DEMO_PAYMENT_TOKEN_SECRET` is a backend-only secret used to deterministically rebuild the same demo payment URL for idempotent retries without storing the raw token.
- This is not a merchant credential.
- Do not commit `.env`.

Firebase Admin credentials:

- real Firestore outside the emulator:
  - set `GOOGLE_APPLICATION_CREDENTIALS` to a local service-account JSON path
- Firestore emulator:
  - no production credential is required

Never commit:

- `.env`
- `DEMO_PAYMENT_TOKEN_SECRET`
- copied service-account JSON

## Run Locally

Terminal 1:

```powershell
cd D:\KhoaHoc\android58\PizzaTime\payment-backend
npm ci
npm run dev
```

Health check:

```powershell
Invoke-WebRequest http://localhost:8080/health | Select-Object -ExpandProperty Content
```

## Cloudflare Tunnel

Terminal 2:

```powershell
cloudflared tunnel --url http://localhost:8080
```

Quick Tunnel notes:

- it gives a temporary random HTTPS hostname
- the hostname changes every time the tunnel restarts
- this is useful for device testing or scanning the demo QR from another device
- backend and tunnel must stay running while the demo payment page is open
- this is development and demo QA only, not production hosting

With an active tunnel, the payment page URL returned by the create endpoint should resolve under the active public hostname:

- `https://<active-host>/demo/pay/<token>`

No VNPay callback or merchant configuration is required for the active Demo provider.

## Endpoint Contracts

### `POST /api/v1/payments/create`

Headers:

```http
Authorization: Bearer <Firebase ID token>
```

Body:

```json
{
  "orderId": "de-3001",
  "requestId": "req-3001-abcdef"
}
```

Success response:

```json
{
  "paymentAttemptId": "PT...",
  "paymentReference": "PT...",
  "paymentPageUrl": "https://payments.example.test/demo/pay/<token>",
  "qrPayload": "https://payments.example.test/demo/pay/<token>",
  "amountVnd": 123000,
  "expiresAt": "2026-07-16T08:45:00.000Z"
}
```

The backend ignores any client-supplied `amount` or `customerId` fields. The verified Firebase principal and the trusted order snapshot remain authoritative.

### `GET /demo/pay/:token`

Public page that displays:

- `PizzaTime Demo Payment`
- order reference
- amount in VND
- payment reference
- `Confirm Demo Payment`
- `Cancel Payment`
- `For testing purposes only`
- `No real money will be transferred`

`GET` has no side effects. Link previews, QR scanners, and crawlers cannot confirm payment through `GET`.

### `POST /demo/pay/:token/confirm`

Confirms the active demo attempt only when:

- token hash matches one stored attempt
- attempt is still `PENDING`
- attempt is not expired
- attempt is still the order’s active attempt
- order still belongs to the attempt
- trusted amount still matches the stored amount
- order is not already paid

On success:

- `paymentAttempts/{attemptId}.status = PAID`
- `orders/{orderId}.paymentStatus = PAID`
- `orders/{orderId}.paymentProvider = DEMO`

### `POST /demo/pay/:token/cancel`

Cancels only the active pending attempt.

On success:

- `paymentAttempts/{attemptId}.status = FAILED`
- `failureCode = CUSTOMER_CANCELLED`
- `orders/{orderId}.paymentStatus = FAILED` only if this attempt is still current

It never overwrites a `PAID` order.

## Testing

Unit tests:

```powershell
npm test
```

Integration tests with Firestore emulator:

```powershell
npm run test:integration
```

Other checks:

```powershell
npm run lint
npm run typecheck
npm run build
```

The integration script uses `payment-backend/firebase.emulators.json` and a backend-local mirrored `payment-backend/firestore.rules` file so emulator startup stays isolated from root project settings.

## Security Boundaries

- Android cannot sign payment requests.
- Android cannot mark an order as `PAID`.
- Android cannot confirm a demo payment by writing Firestore directly.
- The backend verifies Firebase ID tokens.
- The backend verifies Customer ownership.
- The backend verifies the trusted integer-VND amount from `pricingSnapshotVnd`.
- The backend stores only the SHA-256 hash of the payment token.
- The raw payment token is never stored and is not logged.
- `paymentAttempts` remains backend-only.

## Future Provider Adapter

The code keeps a provider abstraction in place.

- `DemoPaymentProvider` is active today.
- A future `VnPayPaymentProvider` can be added later without changing the backend trust boundary, auth model, order ownership checks, idempotency model, or attempt repository contract.

Task 81B does not add Android checkout networking, Android Custom Tabs, payment-result UI, deep links, or real-provider money movement.
