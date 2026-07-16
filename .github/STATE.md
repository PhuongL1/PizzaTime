# STATE
_updated: 2026-07-16_

## Active
<!-- max 3 tasks. Agent overwrites this entire file each update. -->

### task-20260702-81c
task: "Implement automated demo qr payment flow and end-to-end QA"
type: feat
phase: DONE
plan: "Resume from Task 81B, add one truthful Demo QR Payment checkout path with persistent request-id state, trusted backend session creation, QR/browser/deep-link support, Firestore-driven payment observation, exactly-once cart clearing and success navigation, then verify backend, rules, Android, and static security scans before one Task 81C commit"
approach: "Keep Firestore order paymentStatus as the only payment truth, never let Android mark PAID or send a trusted amount, keep DEMO visibly labeled as simulated, reuse the Task 81B backend session/idempotency contract, persist only non-secret customer-scoped pending-payment state, and extend the existing notification/deep-link infrastructure instead of creating parallel systems"
files: ".github/STATE.md, .github/audits/task-20260702-81c-demo-payment-android-audit.md, app/build.gradle.kts, gradle/libs.versions.toml, app/src/debug/AndroidManifest.xml, app/src/debug/res/xml/debug_network_security_config.xml, app/src/customer/AndroidManifest.xml, checkout/payment/order-detail/notification/navigation Kotlin files, payment-backend Android-integration files/tests, and Android unit tests for Demo payment config/contract/pending-store coverage"
review: 3/3
qa: 4/5
note: "Branch feature/81-vnpay-handoff; base commit d7962c7 verified; Demo QR Payment checkout, persistent pending-payment recovery, return-to-app deep link routing, Continue Payment / Create New Payment, and exactly-once Payment received + cart-clear + Order Success flow are implemented. Backend lint/typecheck/unit/integration/build passed, rules tests passed, six-flavor debug unit tests passed, six debug assemblies passed, full Gradle build passed, and Toast/secret/logging/unsafe-URI scans passed. Honest remaining gap is local end-to-end device+tunnel QA because payment-backend/.env is absent, no public tunnel is active, and adb shows no attached device."

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
