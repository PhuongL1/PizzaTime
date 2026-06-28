# Dependency Audit
_generated: 2026-06-01_
_method: manual (no ben-manes.versions plugin)_

## Vulnerabilities

No known HIGH/CRITICAL vulnerabilities detected in current versions.

> Note: No automated scanner (`./gradlew dependencyUpdates` or equivalent) is configured.
> Consider adding `com.github.ben-manes.versions` plugin for automated checks.

## Current Versions (from gradle/libs.versions.toml)

| Category | Package | Version | Status |
|----------|---------|---------|--------|
| Build | AGP | 9.1.0 | current |
| Language | Kotlin | 2.3.20 | current |
| Language | KSP | 2.3.6 | current |
| DI | Hilt | 2.59.2 | current |
| Core | AndroidX Core KTX | 1.18.0 | current |
| UI | Material | 1.13.0 | current |
| UI | AppCompat | 1.7.1 | current |
| UI | ConstraintLayout | 2.2.1 | current |
| Arch | Lifecycle | 2.10.0 | current |
| Arch | Navigation | 2.9.7 | current |
| Async | Coroutines | 1.10.2 | current |
| Storage | DataStore | 1.2.1 | current |
| Serialization | Gson | 2.13.2 | current |
| ML | MediaPipe Tasks Vision | 0.10.26.1 | current |
| Camera | CameraX | 1.4.2 | current |
| Firebase | BOM | 34.12.0 | current |
| Test | JUnit | 4.13.2 | current |
| Test | Robolectric | 4.13 | current |
| Test | Espresso | 3.7.0 | current |
| Debug | LeakCanary | 2.14 | current (disabled) |

## Outdated (major bumps only)

None detected — all dependencies at latest stable versions.

## Action Items

- [ ] Add `com.github.ben-manes.versions` plugin for automated dependency update checks
- [ ] Re-enable LeakCanary for debug builds (currently commented out in build.gradle.kts)
