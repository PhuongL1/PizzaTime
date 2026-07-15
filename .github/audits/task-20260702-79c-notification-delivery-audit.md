# Task 79C Notification Delivery Audit

Date: 2026-07-15
Branch: `feature/79-notification-ux`
Foundation: `e1875c1 feat(task-20260702-79b): add unread notification badges to menu buttons`

## Current authoritative path

Firestore listeners and `NotificationCatchUpWorker` create order/review events through
`NotificationEventFactory`. FCM currently constructs `AppNotification` directly. All three call
`NotificationDispatcher`, which checks `NotificationStateStore`, writes `NotificationInboxStore`,
records the dedupe key, then selects one foreground or background delivery path. The background
path has one direct `NotificationManagerCompat.notify()` owner in `PizzaTimeNotificationManager`.

`NotificationInboxStore` is the unread source of truth. Its persisted key is isolated by
`BuildConfig.APPLICATION_ID`, app edition, Firebase UID, and role. Mark-read and mark-all-read
publish the active list immediately, and Task 79B derives every visible `btnMenu` badge from that
list.

## Event matrix

| Event source | Role | Notification type / trigger | Current dedupe key | Inbox | Foreground | Background | Deep link | Mark read / badge | Work / FCM coverage | Known limitation |
|---|---|---|---|---|---|---|---|---|---|---|
| Firestore order listener | Customer | CONFIRMED | `order:<orderId>:status:CONFIRMED:<eventMillis>` | Dispatcher | UiMessage | System card | Order tracking | Tap marks exact ID; badge Flow updates | Worker mirrors; FCM accepts generic type | FCM fallback key can differ |
| Firestore order listener | Customer | PREPARING or BAKING | `order:<orderId>:status:<status>:<eventMillis>` | Dispatcher | UiMessage | System card | Order tracking | Exact ID | Worker mirrors; FCM generic | FCM does not validate canonical status |
| Firestore order listener | Customer | READY_FOR_DELIVERY | `order:<orderId>:status:READY_FOR_DELIVERY:<eventMillis>` | Dispatcher | UiMessage | System card | Order tracking | Exact ID | Worker mirrors; FCM generic | Same event can use remote fallback key |
| Firestore order listener | Customer | ASSIGNED_TO_SHIPPER fallback | `order:<orderId>:status:ASSIGNED_TO_SHIPPER:<eventMillis>` | Dispatcher | UiMessage | System card | Order tracking | Exact ID | Worker mirrors; FCM generic | Backend sender absent |
| Firestore order listener | Customer | DELIVERING | `order:<orderId>:status:DELIVERING:<eventMillis>` | Dispatcher | UiMessage | System card | Order tracking | Exact ID | Worker mirrors; FCM generic | Backend sender absent |
| Firestore order listener | Customer | DELIVERED | `order:<orderId>:status:DELIVERED:<eventMillis>` | Dispatcher | UiMessage | System card | Customer order detail | Exact ID | Worker mirrors; FCM generic | Destination validates document after navigation |
| Firestore order listener | Customer | CANCELLED, bounded reason fallback | `order:<orderId>:status:CANCELLED:<eventMillis>` | Dispatcher | UiMessage | System card | Customer order detail | Exact ID | Worker mirrors; FCM generic | Remote reason is not normalized |
| Firestore order listener | Staff | Genuine new PENDING after baseline | `staff:new-order:<orderId>:<eventMillis>` | Dispatcher | UiMessage | System card | Staff order detail | Exact ID | Worker mirrors; FCM generic | FCM can claim an incompatible role/type |
| Firestore order listener | Kitchen | Genuine CONFIRMED after baseline | `order:<orderId>:status:CONFIRMED:<eventMillis>` | Dispatcher | UiMessage | System card | Kitchen order detail | Exact ID | Worker mirrors; FCM generic | FCM can claim an incompatible role/type |
| Firestore order listener | Shipper | Genuine READY_FOR_DELIVERY after baseline | `order:<orderId>:status:READY_FOR_DELIVERY:<eventMillis>` | Dispatcher | UiMessage | System card | Shipper detail | Exact ID | Worker mirrors; FCM generic | Existing assignment guard remains repository-owned |
| Firestore order listener | Admin | DELIVERED or CANCELLED | Canonical order status key | Dispatcher | UiMessage | System card | Existing admin/manage-orders fallback | Exact ID | Worker mirrors; FCM generic | No dedicated admin order-detail screen |
| Firestore productReviews listener | Admin | New product review after baseline | `review:<reviewId>` | Dispatcher | UiMessage | Reviews-channel card | Existing admin management fallback | Exact ID | Worker mirrors; FCM generic | No dedicated review-management destination |
| WorkManager catch-up | All authenticated role editions | Same supported matrix after saved baseline | EventFactory keys | Dispatcher | UiMessage only if process STARTED | System card otherwise | Same as live listener | Exact ID | Queries every 15+ minutes | Delayed and inexact, not realtime |
| FCM client | All authenticated role editions | Generic `type/title/body` data or notification fallback | Caller key or sent-time fallback | Dispatcher | UiMessage | Own card, while Play services may render notification payload | Caller/default deep link | Exact ID | Client only | Notification payload can bypass inbox/dedupe; no trusted sender exists |

## Baseline and catch-up behavior

- First Firestore snapshots seed order/review state without user-visible historical events.
- The first Worker pass seeds state when no saved sync marker exists.
- Subsequent listener and Worker passes use `NotificationEventFactory` and the dispatcher.
- Periodic work is unique per application/account/role, uses the 15-minute platform minimum, and
  requires network access.
- The Worker resolves whichever account is active when it runs instead of validating the scope for
  which it was scheduled. A cancelled old-account worker can race an account switch.

## Duplicate and loss risks found

1. Dispatcher check, inbox write, and dedupe write are separate unsynchronized operations. Concurrent
   Firestore, Worker, and FCM callbacks can pass the same check.
2. FCM directly constructs events and permits a sent-time fallback key, so it does not share the
   deterministic EventFactory contract used by Firestore and WorkManager.
3. FCM accepts notification payload title/body. Google Play services can render notification payloads
   independently in background, bypassing the application inbox and dedupe path.
4. `NotificationEventBus` retains callbacks globally, has no observers, and adds a second publish path
   with no product behavior.
5. Inbox persistence reports no acceptance result; dedupe is recorded unconditionally after the call.
6. A missing/corrupt dedupe store can replay an existing inbox event, and replacement resets read state.

## Routing and security risks found

1. PendingIntent extras omit application ID, recipient UID, and recipient role.
2. Intent capture infers intended role from the receiving session/edition rather than immutable scope.
3. Pending routing is persisted without account identity, so account switch can consume another
   account's destination when IDs overlap.
4. `onNewIntent` attempts immediate navigation without checking `FragmentManager.isStateSaved`.
5. Deep-link handling trusts destination extras and marks by ID without confirming the exact active
   inbox record matches the request.
6. Admin order/review links use staff detail or manage-orders even when a safer admin destination exists.
7. System IDs use `String.hashCode()`, with avoidable collision risk, and intents lack unique data.
8. Token logging includes full UID and a token suffix.

## Permission, channels, and manifest

- Main manifest declares `POST_NOTIFICATIONS` and exactly one non-exported FCM service.
- API 33+ permission uses Activity Result and is gated to authenticated role home fragments.
- Guest/signed-out scope cannot prompt. A package/account preference prevents repeated prompts.
- Denial leaves inbox/badge/foreground behavior active and suppresses only system posting.
- Stable channels are `pizzatime_order_updates` and `pizzatime_reviews`; creation is idempotent and
  string-resourced. `ic_notification_pizza` is a monochrome small icon.
- The rationale uses a platform `AlertDialog`; Task 79C will use Material styling without changing timing.

## Task 79C delivery contract

1. Resolve and validate current application/account/role scope.
2. Validate immutable event identity and canonical dedupe key.
3. Under one synchronized processor, reject if dedupe metadata or the scoped inbox has the event.
4. Persist the scoped inbox mutation; only continue when accepted.
5. Record bounded dedupe metadata and publish the unread list.
6. Snapshot process foreground state once.
7. Foreground: publish one `UiMessage`; never call the system manager.
8. Background: attempt one system card; never publish foreground UI. Permission denial suppresses only
   the card, not inbox persistence.

FCM will support strict data-only payloads and derive local copy/destination through
`NotificationEventFactory`. Worker input will carry its scheduled scope and exit when the active scope
does not match. PendingIntents will carry sanitized primitive scope/destination fields and be resolved
against the exact active inbox record before navigation and mark-read.

## Truthful platform limitation

No trusted Firebase Admin SDK, Cloud Functions, or FCM HTTP v1 sender exists in this repository.
Firestore listeners provide live delivery while the process is alive, WorkManager provides delayed
inexact catch-up, and the Android FCM client can receive future trusted data messages. Immediate realtime
delivery after full process death is not guaranteed by Task 79C.

## Implemented result

- Firestore, WorkManager, and strict FCM data events now converge on `NotificationEventFactory`,
  `NotificationDeliveryProcessor`, the scoped inbox, and `NotificationDispatcher`.
- The synchronized processor validates the active application/account/role and canonical identity,
  rejects either a persisted inbox duplicate or bounded dedupe match, persists first, and snapshots
  exactly one foreground/background delivery path.
- Foreground delivery publishes one `UiMessage`; background delivery attempts one system notification.
  Permission denial suppresses the system card without suppressing inbox persistence or badge updates.
- FCM notification payloads are rejected by the application pipeline to avoid competing with Google
  Play services. The supported future backend contract is data-only and must include application,
  recipient, role, type, event time, and canonical dedupe identity.
- Worker input is bound to its scheduled application/account/role and becomes a no-op after an account
  switch. First pass remains a baseline seed and periodic catch-up remains delayed and inexact.
- PendingIntents carry sanitized primitive scope and destination fields. Cold and warm intents resolve
  against the exact active inbox record; only that record is marked read after safe routing.
- System notification IDs, PendingIntent data, and group keys use deterministic SHA-256-derived scoped
  identities. Public lock-screen content is generic and private content remains on the private card.
- Customer notification destinations validate order ownership before rendering order detail or tracking.
- The unused callback-retaining `NotificationEventBus` was removed. No production Toast, View/Context
  singleton retention, backend credential, Firestore rule, or notification channel ID change was added.

## Verification summary

- Three review rounds passed: architecture, security/lifecycle, and product behavior.
- Unit tests passed for all six debug flavors, including delivery routing, cross-source dedupe, payload
  validation, routing isolation, exact read behavior, identity stability, and Worker baseline/scope rules.
- All six debug assemblies and the full Gradle build passed.
- Merged manifests contain `POST_NOTIFICATIONS`, exactly one non-exported FCM service, and an exported
  launcher activity in every flavor.
- Emulator smoke checks installed and launched Customer, Staff, Kitchen, Shipper, and Admin without an
  application crash; stable order/review channels were present. Real cross-role delivery, permission
  denial, notification-tap, and trusted FCM scenarios still require controlled manual environment QA.
