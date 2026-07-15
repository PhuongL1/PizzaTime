# Task 81A Audit — Payment And Delivery Handoff Foundation

Updated: 2026-07-15
Branch: feature/81-vnpay-handoff
Base commit inspected: c40e506

## Scope

Task 81A prepares the order domain, UI policy, transaction policy, notification events, and Firestore Rules required for a future prepaid VNPay flow without adding VNPay checkout, payment credentials, QR generation, backend signing, or client-side payment confirmation.

## Current order and payment architecture

- Order creation currently happens in `feature/customer/checkout/FirebaseOrderRepository.kt`.
- New orders are still written as:
  - `paymentMethod = "CASH_ON_DELIVERY"`
  - `paymentStatus = "UNPAID"`
  - `cashCollected = false`
- Checkout does not expose a real VNPay path yet. Existing visible non-cash payment tiles are “coming soon” placeholders.
- Canonical destination writing already uses `deliveryAddress` plus `deliveryLocation` through `OrderDeliveryDestinationResolver`.
- Staff, kitchen, shipper, and customer order reads each map Firestore documents independently.
- Payment method and payment status display mapping is duplicated in:
  - `CustomerOrderFirestoreRepository`
  - `StaffOrderFirestoreRepository`
  - `ShipperOrderFirestoreRepository`

## Current order schema observed

Current main fields in active use:

- identity:
  - `orderId`
  - `orderCodeKey`
  - `orderCode`
- customer/store:
  - `customerId`
  - `customerEmail`
  - `customerName`
  - `customerPhone`
  - `storeName`
  - `pickupAddress`
  - `pickupLat`
  - `pickupLng`
  - `storePhone`
- status:
  - `status`
  - `statusHistory`
  - `updatedAt`
  - `createdAt`
  - `cancelledBy`
  - `cancellationReason`
  - `statusReason`
- delivery:
  - `deliveryAddress`
  - `deliveryLocation`
  - legacy support via destination resolver
  - `distanceKm`
  - `deliveryFee`
  - `shipperId`
- totals/items:
  - `items`
  - `itemsSubtotal`
  - `subtotal`
  - `discountAmount`
  - `discount`
  - `promoCode`
  - `finalTotal`
  - `total`
- payment fields currently in production:
  - `paymentMethod`
  - `paymentStatus`
  - `cashCollected`
  - `paidAt`
  - `collectedByShipperId`
  - `collectedAmount`
  - `deliveredAt`

## Current update and transition paths

`feature/order/OrderTransitionRepository.kt` is the canonical client transition repository.

Observed transitions:

- Customer:
  - `PENDING -> CANCELLED`
- Staff:
  - `PENDING -> CONFIRMED`
  - `PENDING/CONFIRMED -> CANCELLED`
- Kitchen:
  - `CONFIRMED -> PREPARING`
  - `PREPARING -> BAKING`
  - `BAKING -> READY_FOR_DELIVERY`
  - kitchen cancellation paths
- Shipper:
  - `READY/READY_FOR_DELIVERY -> ASSIGNED_TO_SHIPPER`
  - `READY/READY_FOR_DELIVERY/ASSIGNED_TO_SHIPPER -> DELIVERING`
  - `DELIVERING -> DELIVERED`

Current critical issue:

- Shipper completion currently client-writes:
  - `paymentMethod = "CASH_ON_DELIVERY"`
  - `paymentStatus = "PAID"`
  - `paidAt = serverTimestamp`
  - `cashCollected = true`
  - `collectedByShipperId`
  - `collectedAmount`
  - `deliveredAt`

This violates Task 81A because Android must not mark prepaid payment as `PAID`, and COD should no longer depend on a fake unpaid/paid progression.

## Current Customer order-detail actions

`feature/customer/orderdetail/CustomerOrderDetailFragment.kt` currently provides:

- Reorder
- Support
- Cancel order
- Rate order

Current gaps:

- no owner-only receipt confirmation action
- no live order listener; detail is loaded once with `loadOrderDetail(...)`
- no handoff state presentation

## Current Shipper delivery actions

`feature/shipper/detail/ShipperDeliveryDetailFragment.kt` currently provides:

- Start Delivery
- generic delivered/cash-collected completion
- Resume Tracking
- external navigation
- map rendering and Epic 80 tracking integration

Current gaps:

- one generic action button is status-only, not payment/handoff aware
- no prepaid “I’ve Arrived” transition
- no prepaid “Waiting for Customer Confirmation” state
- no prepaid completion gate after customer confirmation

## Current Staff confirmation behavior

`StaffOrderFirestoreRepository.updateOrderStatus(...)` forwards confirm requests to `OrderTransitionRepository.confirmByStaff(...)`.

Current gap:

- no payment-aware block; unpaid VNPAY would currently be confirmable if such an order existed

## Current Delivered authorization and security model

`firestore.rules` currently:

- forces order create to COD-only with:
  - `paymentMethod == "CASH_ON_DELIVERY"`
  - `paymentStatus == "UNPAID"`
- allows shipper update fields:
  - `paymentStatus`
  - `paymentMethod`
  - `paidAt`
  - `deliveredAt`
  - `collectedByShipperId`
  - `collectedAmount`
  - `cashCollected`
- explicitly validates the COD collection write during `DELIVERING -> DELIVERED`

Current risks:

- client payment mutation is allowed
- no prepaid handoff transition model exists
- no owner-only customer receipt transition exists
- no exact field restriction for prepaid arrival/confirmation/completion exists

## Current notification event support

Task 79 notification support is already in place through:

- `NotificationEventFactory`
- `NotificationEventContract`
- `OrderNotificationMonitor`
- `NotificationCatchUpWorker`
- inbox dedupe and role-scoped deep links

Current supported order notifications are status-history driven only:

- customer confirmed / preparing / ready / assigned / delivering / delivered / cancelled
- staff new pending order
- kitchen confirmed order
- shipper ready for delivery
- admin delivered / cancelled

Current gaps:

- no handoff transition event for `LOCKED -> AWAITING_CUSTOMER`
- no handoff transition event for `AWAITING_CUSTOMER -> CUSTOMER_CONFIRMED`

## Legacy-order compatibility plan

Task 81A needs conservative fallback behavior:

- missing `paymentMethod` => treat as COD
- missing COD `paymentStatus` => treat as `NOT_REQUIRED`
- missing VNPAY `paymentStatus` => treat as `PENDING`
- missing COD `deliveryHandoffStatus` => treat as `NOT_REQUIRED`
- missing VNPAY `deliveryHandoffStatus` => treat as `LOCKED`
- unknown enum strings must not crash; they should map conservatively for UI/policy
- old orders must not be rewritten on read

## Proposed payment schema

Canonical fields:

- `paymentMethod: String`
- `paymentStatus: String`
- `paymentProvider: String?`
- `paymentAttemptId: String?`
- `paymentReference: String?`
- `paidAt: Timestamp?`
- `providerTransactionId: String?`

Task 81A write behavior:

- new COD orders:
  - `paymentMethod = COD`
  - `paymentStatus = NOT_REQUIRED`
- future VNPay creation prepared but not exposed:
  - `paymentMethod = VNPAY`
  - `paymentStatus = PENDING`
- no real provider values populated
- no client-side `PAID` write

## Proposed handoff schema

Canonical fields:

- `deliveryHandoffStatus: String`
- `shipperArrivedAt: Timestamp?`
- `customerReceivedAt: Timestamp?`
- `customerReceiptConfirmedBy: String?`
- `deliveryCompletedAt: Timestamp?`

Task 81A write behavior:

- new COD orders:
  - `deliveryHandoffStatus = NOT_REQUIRED`
- future VNPay orders:
  - `deliveryHandoffStatus = LOCKED`

## Proposed transition matrix

- Staff confirm:
  - COD: existing status rule remains
  - VNPAY: only when `paymentStatus == PAID`
- Shipper start delivery:
  - existing assignment/status rules remain
  - VNPAY additionally requires `paymentStatus == PAID`
- Shipper arrived:
  - VNPAY + PAID + DELIVERING + LOCKED + assigned shipper
  - update only handoff arrival fields
- Customer confirm receipt:
  - VNPAY + PAID + DELIVERING + AWAITING_CUSTOMER + owner customer
  - does not set `DELIVERED`
- Shipper complete prepaid delivery:
  - VNPAY + PAID + DELIVERING + CUSTOMER_CONFIRMED + assigned shipper
  - atomically sets `deliveryHandoffStatus = COMPLETED`
  - atomically sets `status = DELIVERED`
- COD completion:
  - existing direct delivery completion behavior remains
  - no customer receipt step required

## Proposed UI state matrix

Customer order detail:

- COD:
  - hide prepaid receipt action
- VNPAY + PAID + DELIVERING + LOCKED:
  - show disabled “Confirm Order Received”
- VNPAY + PAID + DELIVERING + AWAITING_CUSTOMER:
  - show enabled “Confirm Order Received”
- after confirmation / delivered:
  - disabled “Order Received”

Shipper delivery detail:

- COD:
  - preserve direct completion flow
- VNPAY + PAID + DELIVERING + LOCKED:
  - enabled “I’ve Arrived”
  - disabled “Complete Delivery”
- VNPAY + PAID + DELIVERING + AWAITING_CUSTOMER:
  - disabled “Waiting for Customer Confirmation”
  - disabled “Complete Delivery”
- VNPAY + PAID + DELIVERING + CUSTOMER_CONFIRMED:
  - enabled “Complete Delivery”
- delivered:
  - disabled “Delivery Completed”

## Proposed notification events

New Task 81A notifications should be derived from Firestore order snapshots and existing dedupe logic:

1. arrival transition
   - `LOCKED -> AWAITING_CUSTOMER`
   - recipient: owning customer
   - dedupe: `handoff:<orderId>:arrived:<transitionTimestamp>`

2. customer confirmation transition
   - `AWAITING_CUSTOMER -> CUSTOMER_CONFIRMED`
   - recipient: assigned shipper
   - dedupe: `handoff:<orderId>:customer-confirmed:<transitionTimestamp>`

No second notification system should be introduced.

## Proposed Firestore Rules changes

- allow COD create with canonical `COD/NOT_REQUIRED/NOT_REQUIRED`
- prepare future VNPAY create only as `VNPAY/PENDING/LOCKED`
- deny any client update of:
  - `paymentStatus`
  - `paidAt`
  - `paymentProvider`
  - `paymentAttemptId`
  - `paymentReference`
  - `providerTransactionId`
- staff confirm on VNPAY only if `paymentStatus == PAID`
- add exact shipper arrival transition rule
- add exact customer receipt confirmation rule
- add exact prepaid shipper completion rule
- preserve Epic 80 tracking subcollection rules
- keep parent order rules narrow; no broad wildcard relaxation

## Files expected to change

- `.github/STATE.md`
- `.github/audits/task-20260702-81a-payment-handoff-audit.md`
- `app/src/main/java/com/devpro/pizzatime/feature/order/*`
- `app/src/main/java/com/devpro/pizzatime/feature/customer/checkout/FirebaseOrderRepository.kt`
- `app/src/main/java/com/devpro/pizzatime/feature/customer/orderhistory/CustomerOrderFirestoreRepository.kt`
- `app/src/main/java/com/devpro/pizzatime/feature/customer/orderdetail/*`
- `app/src/main/java/com/devpro/pizzatime/feature/staff/StaffOrderFirestoreRepository.kt`
- `app/src/main/java/com/devpro/pizzatime/feature/staff/detail/*`
- `app/src/main/java/com/devpro/pizzatime/feature/shipper/ShipperOrderFirestoreRepository.kt`
- `app/src/main/java/com/devpro/pizzatime/feature/shipper/detail/*`
- `app/src/main/java/com/devpro/pizzatime/core/notification/*`
- `app/src/main/res/layout/fragment_customer_order_detail.xml`
- `app/src/main/res/layout/fragment_shipper_delivery_detail.xml`
- `app/src/main/res/values/strings.xml`
- `firestore.rules`
- `firebase-rules-tests/test/*`
- `app/src/test/java/com/devpro/pizzatime/feature/order/*`
- `app/src/test/java/com/devpro/pizzatime/core/notification/*`

## Regression risks

1. COD regression if legacy `UNPAID`/`PAID` assumptions remain in any mapper or UI formatter.
2. Staff confirmation regression if payment-aware gating is only added in UI and not transaction/rules.
3. Shipper delivery regression if prepaid states break existing COD direct completion.
4. Duplicate inbox notifications if handoff events are emitted from both live listener and catch-up paths without canonical dedupe.
5. Tracking regression if prepaid arrival accidentally stops Epic 80 foreground tracking before `DELIVERED`.
6. Cross-account authorization regression if customer receipt confirmation is not enforced in both repository and Rules.
7. Legacy order rendering regression if unknown/missing payment fields are parsed unsafely.
