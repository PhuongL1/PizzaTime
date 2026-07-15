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

### task-20260702-79d
task: "Remove all production Toast usage with professional feedback"
type: refactor
phase: IN_REVIEW
plan: "Complete customer, role, admin, and core Toast migrations in four gated commits"
approach: "Use shared Snackbar feedback, inline validation, Material confirmations, empty states, and a lifecycle-collected foreground message bus while preserving background system notifications."
files: "37 Toast-bearing Kotlin files, core UI message support, MainActivity, minimal resources, Toast audit"
review: 1/3
qa: 1/3
review: 2/3
qa: 2/3
note: "79D1 scoped Toast search, Guest/Customer assembly, Customer unit tests, lint, and two diff reviews pass"

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
