# Codebase Context
_generated: 2026-06-01_
_codegraph: enabled_

---

## TECH
> Language, framework, tooling — what the project runs on

- **Language:** Kotlin (primary) + C++ (native fluid simulation via NDK)
- **Framework:** Android SDK, Jetpack (Navigation 2.9, Lifecycle 2.10, ViewBinding, DataBinding, DataStore), Hilt 2.59 (DI), MediaPipe (hand gesture recognition), CameraX 1.4
- **Runtime:** JVM (Android, minSdk 28, targetSdk 36) + NDK/CMake (OpenGL ES compute)
- **Build:** Gradle 9.1 (Kotlin DSL) + CMake 3.22.1 (native)
- **Test:** JUnit4 + Robolectric 4.13 + Espresso — run: `./gradlew test`
- **Package manager:** Gradle version catalog (`gradle/libs.versions.toml`)
- **Firebase:** Analytics, Crashlytics, Performance

---

## ARCH
> Architecture pattern, folder structure, main data flow

- **Pattern:** MVVM + MVI-style intents (sealed class UiIntent → ViewModel → StateFlow/UiEffect → Fragment)
- **Source root:** `app/src/main/`
- **Entry points:**
  - `FluidWallpaperApp.kt` — Application class (@HiltAndroidApp)
  - `MainActivity.kt` — main Activity (hosts nav graph)
  - `FluidWallpaperService.kt` — WallpaperService for live wallpaper
  - `native-lib.cpp` — JNI bridge entry (C++ ↔ Kotlin)

| Folder | Role |
|--------|------|
| `ui/home/` | Home screen — preset gallery |
| `ui/diy/` | DIY editor — slider params, color picker, effects |
| `ui/favorites/` | Favorites management |
| `ui/settings/` | App settings |
| `ui/gesture/` | Hand gesture control (MediaPipe + CameraX) |
| `ui/fluid/` | GLSurfaceView + Renderer (OpenGL bridge) |
| `ui/subscription/` | Subscription / paywall dialog |
| `ui/premium/` | Upgrade bottom sheet |
| `ui/common/` | Base classes, adapters, extensions |
| `data/model/` | Domain models (Preset, ExperienceTab, FavoriteEntry) |
| `data/repository/` | Data access (PresetRepository, EffectsRepository, FavoriteRepository) |
| `data/store/` | UserPresetStore (DataStore persistence) |
| `data/nativebridge/` | FluidSplatPort, FluidStateProbe, NativeBridgeModule |
| `data/premium/` | EntitlementService, PremiumStore, FeatureGate |
| `data/effects/` | Effect param data classes (Dithering, Emboss, etc.) |
| `data/gesture/` | GestureRepository |
| `domain/premium/` | EntitlementGuard, GuardResult |
| `di/` | Hilt modules (per-effect DI) |
| `jni/` | NativeLib.kt — JNI declarations (42 native methods) |
| `service/` | FluidWallpaperService, WallpaperGLThread |
| `util/` | Helpers (ColorHarmonyGenerator, WallpaperHelper, PerfTracer) |
| `cpp/` | Native C++ engine: FluidSolver, FluidRender, PostFx, ShaderManager |
| `cpp/effects/` | C++ post-processing effects (Ascii, Dithering, Emboss, Neon, etc.) |
| `cpp/gfx/` | GPU utilities (FBO, DoubleFBO, Blit, FormatProbe) |
| `cpp/shaders/` | GLSL shaders embedded as C headers (physics, postfx, render) |

---

## QUALITY
> Conventions used in the codebase — so agents follow the right style

- **Naming:** camelCase (Kotlin properties/functions), PascalCase (classes/enums), UPPER_SNAKE (constants), m_ prefix (C++ members)
- **Error handling:** Sealed class results (`GuardResult.Allowed / Blocked`), no exceptions for control flow
- **Async:** Kotlin Coroutines + StateFlow (UI state), SharedFlow (one-shot effects), viewModelScope
- **Comment style:** Phase-tagged comments (`// Phase N:` — tracks development increments), KDoc for public APIs
- **Test pattern:** Unit tests (Robolectric for Android context), instrumented tests (Espresso) — moderate coverage on ViewModels/mappers

---

## CONCERNS
> Special notes, quirks, debt, or risks agents should know

- Native C++ engine has no unit tests — correctness verified visually + GPU smoke test harness
- `native-lib.cpp` has 52 symbols / many JNI methods — large surface, fragile to refactor
- LeakCanary debug dependency is commented out (disabled)
- Proguard/R8 minification disabled (`isMinifyEnabled = false`) — not release-ready
- `data/effects/` has 11 nearly identical param data classes — could consolidate but works fine
- Web prototype (`web_prototype/`) is reference-only, not deployed

---

## SYMBOLS
> Key symbols from codegraph index — agents use for quick lookup

| Symbol | Kind | File | Notes |
|--------|------|------|-------|
| `FluidSolver` | class | cpp/FluidSolver.h | Core physics simulation (73 symbols) |
| `FluidRender` | class | cpp/FluidRender.h | Rendering pipeline (48 symbols) |
| `FluidRenderer` | class | ui/fluid/FluidRenderer.kt | Kotlin GLSurfaceView.Renderer |
| `NativeLib` | class | jni/NativeLib.kt | JNI declarations (42 native methods) |
| `DiyViewModel` | class | ui/diy/DiyViewModel.kt | DIY editor state (72 symbols) |
| `GestureScreenViewModel` | class | ui/gesture/GestureScreenViewModel.kt | Gesture control (73 symbols) |
| `DiyActivity` | class | ui/diy/DiyActivity.kt | DIY editor Activity (98 symbols) |
| `GestureControlActivity` | class | ui/gesture/GestureControlActivity.kt | Gesture screen (101 symbols) |
| `MainActivity` | class | MainActivity.kt | App entry (46 symbols) |
| `FluidWallpaperService` | class | service/FluidWallpaperService.kt | Live wallpaper service |
| `PresetRepository` | class | data/repository/PresetRepository.kt | Preset data access (34 symbols) |
| `PostFx` | class | cpp/effects/PostFx.h | Post-processing pipeline (28 symbols) |
| `FluidConfig` | class | data/FluidConfig.kt | Kotlin config model (25 symbols) |
| `EntitlementGuard` | class | domain/premium/EntitlementGuard.kt | Premium feature gating |

_Index: 482 files · 4,866 nodes · 8,831 edges (from codegraph status)_
