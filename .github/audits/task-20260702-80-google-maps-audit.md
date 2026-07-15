# Epic 80 Billing-Free OpenStreetMap Delivery Tracking Audit

Date: 2026-07-15
Branch: `feature/80-google-maps`
Base: `c0432c48cc673a8e652830a809f2f311e11aed3e`

## Architecture decision

The prior Google Maps SDK and Google Routes design is cancelled. The former
`MAPS_CONFIGURATION_BLOCKED` state is not an implementation gate. Epic 80 uses the project's existing
osmdroid/OpenStreetMap map renderer, Android `LocationManager`, a Shipper foreground location service,
and one private Firestore current-location document. An installed external navigation application owns
real road routing and ETA. PizzaTime neither calculates nor claims a road route or ETA and labels every
in-app geographic distance as **Straight-line distance**.

No Google Maps SDK, Maps metadata/key, Routes/Directions API, Places/Roads/Distance Matrix API, Google
Cloud billing, Cloud Functions route backend, MapLibre, fake road polyline, fake destination, or fake ETA
is part of this design.

## Existing OpenStreetMap and location architecture

- `org.osmdroid:osmdroid-android:6.1.20` is already the only embedded map engine.
- `MapPickerFragment` uses `MapView`, MAPNIK tiles, `MapEventsOverlay`, an Activity Result permission
  launcher, and platform GPS/network `LocationManager` providers.
- The picker currently sets osmdroid's user agent from a Fragment context instead of configuring it once
  from application identity. It has no explicit app-wide cache policy or shared attribution treatment.
- The picker calls `onResume`, `onPause`, and `onDetach`, but retains a `Marker` field after the view is
  destroyed. Its timeout/listener state also needs stricter view-lifecycle cleanup.
- Its current no-coordinate state centers a hardcoded Hanoi coordinate. Even though it does not persist
  that point automatically, it is a misleading production fallback and must be removed.
- Current-location lookup accepts any last-known sample without an age/coordinate check and registers one
  listener with multiple providers. Registration and removal need bounded, idempotent behavior.
- The picker returns address/latitude/longitude in one Fragment Result and is shared by Customer checkout
  and Admin store settings. That safe primitive result channel will remain.
- The main manifest already declares coarse/fine location. There is no background-location permission,
  foreground location service, or embedded Google map configuration.

## Destination data audit and stale-coordinate risk

Current orders use `deliveryAddress`, `deliveryLat`, and `deliveryLng`. `FirebaseOrderRepository` is the
production creator. `CheckoutFragment` holds the address and coordinates in separate mutable fields,
loads the same flat values from the Customer profile, and writes them back after a picker result.
Changing saved address text in the account flow can leave the previous coordinate pair attached, so the
old fields cannot safely be treated as one confirmed selection.

The new immutable selection pairs a normalized address with one validated coordinate. A manual address
change that no longer matches the selected address invalidates the coordinate. Checkout never attaches
coordinates chosen for a previous address, never derives trusted coordinates from text, and never
overwrites a valid selection after geocoding failure. Map selection can remain usable when reverse
geocoding is unavailable because the existing explicitly entered label is preserved.

## Canonical delivery destination

New order writes use:

```text
deliveryAddress: String
deliveryLocation: GeoPoint
```

`deliveryLocation` is authoritative. The shared data mapper validates finite latitude `[-90, 90]` and
longitude `[-180, 180]`, reads canonical `deliveryLocation` first, then valid legacy
`deliveryLat`/`deliveryLng`, otherwise returns unavailable. New orders do not write the legacy pair.
Existing orders are not migrated or fabricated. Store pickup coordinates are a separate existing schema.
All role screens keep reading the parent order fields they already use; map consumers use the shared
destination resolver.

The existing `distanceKm` is a Haversine value used for the delivery-fee calculation. It is straight-line
distance only and must not be described as road/route/driving distance or ETA.

## Shipper access and delivery-detail map

The current Shipper repository maps legacy destination fields and the detail screen exposes only a basic
external `geo:` action. Order transition transactions already claim/validate `shipperId` and protect
`READY[_FOR_DELIVERY]`/`ASSIGNED_TO_SHIPPER -> DELIVERING -> DELIVERED`. Those transactions remain the
authority; viewing a map never changes status.

Delivery Detail will show the validated destination and a foreground one-shot device location, fit both
markers initially, preserve the camera after manual interaction, expose a Center action, and explicitly
label straight-line distance. Missing permission/provider/location leaves the destination usable and
shows a professional resource-backed state. An old order without coordinates keeps its address and shows
`Delivery location is unavailable.`

## External navigation

For a validated coordinate, the Shipper action first constructs a package-scoped
`google.navigation:q=<lat>,<lng>&mode=d` intent only when Google Maps is installed, then falls back to a
generic encoded `geo:0,0?q=<lat>,<lng>(<label>)` intent. With no coordinates it uses an encoded generic
address search; with no destination or handler it reports a professional unavailable state. Only
validated destination/address data enters the URI—never phone, notes, order objects, or Firestore URIs.
The external application, not PizzaTime, provides the road route and ETA.

## Canonical live-tracking schema

Exactly one bounded document is used; there is no location history:

```text
orders/{orderId}/tracking/current
  shipperId: String
  location: GeoPoint
  accuracyMeters: Number
  bearingDegrees: Number?          # omitted unless valid
  speedMetersPerSecond: Number?    # omitted unless valid
  recordedAt: Timestamp
  updatedAt: server timestamp
  orderStatus: "DELIVERING"
  schemaVersion: 1
```

It contains no address, phone, Customer/Shipper profile, FCM token, notes, items, arbitrary role, or
parent-order copy.

## Foreground tracking service

`DeliveryTrackingService` is Shipper-only through a source-set manifest overlay. It starts only from a
visible detail flow after the existing transition successfully reaches `DELIVERING`, or after an explicit
Resume action validates an already-delivering order. Eligibility requires authenticated Shipper edition,
active SHIPPER role, matching assigned UID, `DELIVERING`, foreground permission, and an enabled provider.

The service calls `startForeground` promptly with the stable low-importance
`pizzatime_delivery_tracking` channel and a detail deep link. It registers GPS/network callbacks once,
validates age/accuracy/coordinates, writes only after meaningful movement or a bounded maximum interval,
and retains at most one pending sample during a temporary failure. Named constants govern sample age,
accuracy, movement, write interval, and revalidation.

One parent-order listener revalidates assignment/status. Auth/role changes, sign-out, permission
revocation, no provider, deletion, permanent authorization failure, delivery/cancellation/status change,
or assignment change removes updates/listeners, cancels work, stops foreground state, and stops the
service. Logout sends an explicit stop action. Conservative `START_NOT_STICKY` recovery never blindly
resumes stale tracking; an eligible Shipper explicitly resumes from Delivery Detail. The final current
document remains as the last valid sample.

## Customer live listener

Order Tracking validates the signed-in Customer and parent-order ownership before it creates exactly one
listener at `tracking/current`, and only while the order is `DELIVERING` with an assigned Shipper. Status
change, cancellation/delivery, order change, account switch, or `onDestroyView` removes it before another
can be registered. Snapshot callbacks are scoped to the expected order and UID.

The map reuses destination/Shipper markers instead of accumulating overlays. The first valid sample may
fit both; subsequent samples move the marker without overriding a user's pan. A Center action opts back
in. A named freshness threshold preserves the last marker while presenting delayed/stale wording and
locale-safe relative time without a per-second timer. Pre-delivery, no-document, delivered, cancelled,
invalid, and transient-error states remain honest; transient errors retain the last valid marker and show
one Snackbar.

## Firestore Rules

The nested `orders/{orderId}/tracking/{trackingId}` rule permits only document ID `current`. Reads are
limited to the parent owning Customer or assigned Shipper. Creates/updates require an authenticated active
SHIPPER, parent `shipperId` equal to auth UID, parent status `DELIVERING`, payload shipper equality, fixed
status/schema, an exact approved key set, Firestore `latlng`, bounded accuracy/bearing/speed, and valid
timestamps. Client delete is denied. Customer/Staff/other Shipper writes and pre/post-delivery writes are
denied. Parent-order rules are not broadened.

Order creation rules separately accept and validate canonical `deliveryLocation` and stop requiring the
legacy flat pair. Rules emulator tests cover owner/other Customer reads, assigned/other Shipper reads,
status-gated writes, role denial, extra/mismatched/invalid fields, and delete denial. Deployment to
`pizzatime-de04c` is a hard completion gate for 80D/80E.

## Expected production and test changes

- `.github/STATE.md` and this audit
- app initialization and shared coordinate/osmdroid map helpers
- `MapPickerFragment`, its layout, strings, attribution resources, and unit tests
- Checkout selection state, Customer profile destination mapping, order creator, shared destination mapper
- Shipper order model/repository, Delivery Detail Fragment/layout, one-shot location helper, navigation helper
- Shipper manifest overlay, tracking service/repository/policy/state, notification strings/channel/deep link
- logout/session integration and focused Shipper policy tests
- Customer Order Tracking repository/policy/Fragment/layout and focused freshness/lifecycle tests
- `firestore.rules` and an isolated `firebase-rules-tests` project if no existing harness is available

Unrelated product, promo, payment, seed, notification-delivery, unread-badge, flavor application-ID, guest
cart, and order-transition code remains unchanged except for narrow integration points.

## Manual device QA

Device QA must cover Customer picker selection/recreation/address invalidation/order GeoPoint write;
Shipper precise/approximate/denied/provider-disabled map states; marker fitting and explicit straight-line
copy; coordinate/address/no-handler external navigation; foreground notification and bounded writes;
Customer marker updates/freshness; delivered/cancelled/logout cleanup; and unauthorized cross-account
access. Logcat must show no crash, leaked callback/listener, duplicate writer/listener, post-completion
write, raw-coordinate spam, or Toast. If `adb devices` has no attached target, production/test/build/commit
work continues and final status is `DEVICE_QA_PENDING`.
