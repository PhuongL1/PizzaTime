---
applyTo: "**/*.kt"
---

# Kotlin — Coding Conventions

## Style

- Indentation: 4 spaces
- Naming: camelCase (functions/properties), PascalCase (classes/interfaces)
- Max line length: 120
- Trailing commas: yes (in multi-line declarations)

## Patterns

- Use `Result<T>` or sealed class for error handling — never throw exceptions for expected cases
- Do not use `!!` — use `?.let {}`, `requireNotNull()`, or `checkNotNull()` with message
- Prefer `val` over `var`; prefer immutable collections
- Use `data class` for DTOs / state objects
- Use `sealed class` / `sealed interface` for finite states (UI state, navigation events)
- Extension functions for utility — keep them in relevant `*Extensions.kt` files

## Architecture

- **Pattern:** MVVM or MVI
- ViewModel exposes state via `StateFlow` (not `LiveData` unless legacy)
- UI layer observes `StateFlow` with `collectAsStateWithLifecycle()` or `repeatOnLifecycle`
- Single source of truth: Repository → ViewModel → UI
- Domain layer optional — add only when business logic is shared across features

## File structure

```
feature/
├── ui/           (Fragment/Activity/Composable + Adapter)
├── viewmodel/    (ViewModel + UiState sealed class)
├── model/        (data classes, enums)
├── repository/   (Repository interface + impl)
└── di/           (Hilt modules)
```

## Testing

- Naming: `should_doX_when_Y` or `given_X_when_Y_then_Z`
- Use JUnit 5 + Turbine (for Flow testing) + MockK
- ViewModel tests: use `runTest {}` + `Dispatchers.UnconfinedTestDispatcher`

## Libraries

### Coroutines
- Use `viewModelScope` in ViewModel — never create custom CoroutineScope unless necessary
- Use `Dispatchers.IO` for disk/network, never `Dispatchers.Main` for heavy work
- Prefer `Flow` over `suspend` for streams; `suspend` for one-shot operations
- Use `flowOn(Dispatchers.IO)` in repository layer, not in ViewModel
- Cancel structured concurrency properly — never use `GlobalScope`
- Use `supervisorScope` when child failure should not cancel siblings

### Hilt
- Annotate Application class with `@HiltAndroidApp`
- Annotate Activity/Fragment with `@AndroidEntryPoint`
- Provide dependencies via `@Module` + `@InstallIn(SingletonComponent::class)` for singletons
- Use `@HiltViewModel` + `@Inject constructor` (not deprecated `@ViewModelInject`)
- Scope: `@Singleton` for app-wide, `@ViewModelScoped` for ViewModel lifetime
- Never inject Context directly into ViewModel — use `@ApplicationContext` if absolutely needed
- Per-feature modules: one `@Module` per effect/feature (e.g., `DitheringEffectModule.kt`)

### Navigation (Jetpack)
- Single Activity architecture with Fragment destinations
- Define nav graph in `res/navigation/nav_main.xml`
- Navigate via `findNavController().navigate(R.id.action_*)` — never create Fragments directly
- Pass data between destinations via Safe Args or `Bundle`
- Deep links: define in nav graph XML, not in code
- Bottom navigation: wire via `setupWithNavController()`

### DataStore (Preferences)
- Use `DataStore<Preferences>` for key-value storage — never SharedPreferences
- Access via `Flow` — never block on `runBlocking` to read
- Write via `edit { prefs -> prefs[key] = value }` in coroutine
- Define keys as `PreferencesKey` constants in companion object or dedicated file
- Single DataStore instance per file — inject via Hilt `@Singleton`

### ViewBinding / DataBinding
- ViewBinding for simple cases — enable in `buildFeatures { viewBinding true }`
- DataBinding when binding expressions/adapters add value — `buildFeatures { dataBinding true }`
- Use `<layout>` wrapper in XML — keep binding expressions simple (no logic in XML)
- Use `@BindingAdapter` for custom attributes (visibility toggle, formatted text)
- Inflate in Fragment: `onCreateView` → `binding = FragmentXBinding.inflate(inflater)`
- Null binding in `onDestroyView`: `_binding = null`

### MediaPipe (Gesture Recognition)
- Use `GestureRecognizer` from `com.google.mediapipe.tasks.vision`
- Initialize on background thread — model loading is heavy
- Process frames via `recognizeAsync()` — never block UI thread
- Map results to app-specific sealed class (e.g., `GestureRecognitionEvent`)
- Release recognizer in `onDestroy` / lifecycle-aware cleanup
- Debounce rapid gesture changes — use `DiscreteGestureDebouncer`

### CameraX
- Use `ProcessCameraProvider` with lifecycle binding
- Prefer `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` for real-time processing
- Set target resolution, not exact — CameraX picks closest supported
- Handle permission via runtime request before binding use cases
- Combine with MediaPipe: pipe `ImageProxy` → convert to `MPImage` → process

### Gson
- Use `@SerializedName` for JSON field mapping when names differ from Kotlin properties
- Register custom `TypeAdapter` for sealed classes / polymorphic types
- Never use Gson on main thread for large payloads
- Prefer `fromJson(reader, Type)` for streaming large files

## Notes

- Minimum SDK: 28 — use `@RequiresApi` for APIs above 28
- Prefer Kotlin DSL for Gradle (`build.gradle.kts`)
- ProGuard/R8: keep rules for Gson models, Hilt generated code
- Phase-tagged comments: `// Phase N:` — tracks development increments (do not remove)
