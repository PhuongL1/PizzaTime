# STATE
_updated: 2026-07-16_

## Active
<!-- max 3 tasks. Agent overwrites this entire file each update. -->

### task-20260702-81b
task: "Implement provider-neutral demo payment backend"
type: feat
phase: DONE
plan: "Preserve the reusable Task 81B backend foundation, swap the active provider from VNPay-specific callbacks to a provider-neutral DEMO flow, update prepaid domain/rules/tests honestly, then run backend QA, rules QA, Android regression, scans, and one Task 81B commit"
approach: "Keep all trust decisions on the backend, require Firebase ID tokens plus Customer ownership, require a trusted integer-VND snapshot, store only token hashes, keep GET side-effect free, and treat DEMO like a prepaid method without pretending that real-provider money movement exists today"
files: ".github/STATE.md, .github/audits/task-20260702-81b-vnpay-backend-audit.md, payment-backend/**, firestore.rules, payment-backend/firestore.rules, firebase-rules-tests/**, Task 81A payment-domain Kotlin files/tests/resources"
review: 3/3
qa: 5/5
note: "Branch feature/81-vnpay-handoff; base commit cf6584f verified; provider-neutral DemoPaymentProvider is active; backend lint, typecheck, unit tests, emulator integration tests, rules tests, Firestore Rules deploy, six-flavor Android unit tests, six debug assemblies, full Gradle build, Toast scan, client PAID-write review, and secret/logging scans all passed. Honest remaining boundary is manual public-host/tunnel QA only; no real VNPay merchant flow is active."

### task-20260702-81a
task: "Implement payment and prepaid delivery handoff foundation"
type: feat
phase: DONE
plan: "Audit current order/payment/rules/notification flow, add canonical payment and handoff domain with one central policy, enforce transitions through repository transactions and Rules, then verify with rules tests, six-flavor tests/builds, and one Task 81A commit"
approach: "Keep Checkout safely COD-only for now, map legacy orders conservatively, prevent any client PAID write, require customer receipt confirmation only for paid VNPAY deliveries, and reuse the existing notification inbox/deep-link pipeline"
files: ".github/audits/task-20260702-81a-payment-handoff-audit.md, order domain/policy/transition repositories, customer/staff/shipper order detail UI, notification contract/factory/tests, firestore.rules, firebase-rules-tests, strings/resources"
review: 3/3
qa: 3/3
note: "Branch feature/81-vnpay-handoff; rules tests passed, firestore.rules deployed to pizzatime-de04c, six-flavor unit tests passed, six debug assemblies passed, full Gradle build passed, device QA pending because adb shows no attached target"

### task-20260702-80
task: "Implement billing-free OpenStreetMap delivery tracking"
type: feat
phase: DONE
plan: "Deliver five gated phases: harden osmdroid; persist/show canonical destination; external navigation; assigned-Shipper foreground tracking; owning-Customer live map"
approach: "Use osmdroid in PizzaTime, LocationManager for device fixes, deliveryLocation GeoPoint, one orders/{orderId}/tracking/current document, and external map apps for road routing/ETA"
files: ".github/audits/task-20260702-80-google-maps-audit.md, shared location/map helpers, checkout/order mappers, Shipper detail/service, Customer tracking, manifests/resources, firestore.rules, isolated rules tests"
review: 3/3
qa: 5/5
note: "80A 66fc4b3; 80B e91ccbb; 80C f1923da; 80D 39d416b with rules deployed to pizzatime-de04c; 80E ready to commit after six-flavor tests, six debug assemblies, full build, and final review; device QA pending because adb shows no attached target"
