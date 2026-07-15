# STATE
_updated: 2026-07-15_

## Active
<!-- max 3 tasks. Agent overwrites this entire file each update. -->

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

### task-20260602-1
task: "Preserve fluid when changing grid size / dye resolution"
type: fix
phase: THINK_DONE
plan: "Scale-blit dye content in C++ resize(); use nativeResize instead of destroy+init"
files: "FluidSolver.cpp, FluidRenderer.kt"
review: 0/3
qa: 0/3
note: "none"

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

### task-20260605-1
task: "Soạn giáo án Kotlin OOP 1 buổi cho người mới"
type: docs
phase: THINK_DONE
plan: "Biên soạn giáo án 180 phút gồm lý thuyết, demo, bài tập, đáp án, rubric"
approach: "Tập trung 4 trụ cột OOP, dạy theo nhịp Learn -> Code -> Review, có bài tập tăng dần và đáp án chuẩn"
files: "oop.md"
review: 0/3
qa: 0/3
note: "none"
