# STATE
_updated: 2026-07-15_

## Active
<!-- max 3 tasks. Agent overwrites this entire file each update. -->

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
phase: IMPLEMENT_80E
plan: "Deliver five gated phases: harden osmdroid; persist/show canonical destination; external navigation; assigned-Shipper foreground tracking; owning-Customer live map"
approach: "Use osmdroid in PizzaTime, LocationManager for device fixes, deliveryLocation GeoPoint, one orders/{orderId}/tracking/current document, and external map apps for road routing/ETA"
files: ".github/audits/task-20260702-80-google-maps-audit.md, shared location/map helpers, checkout/order mappers, Shipper detail/service, Customer tracking, manifests/resources, firestore.rules, isolated rules tests"
review: 2/3
qa: 4/5
note: "80A 66fc4b3; 80B e91ccbb; 80C f1923da; 80D implemented with shipper-only FGS tracking, rules tests passing, and firestore.rules deployed to pizzatime-de04c; moving to owning-Customer live map"

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
