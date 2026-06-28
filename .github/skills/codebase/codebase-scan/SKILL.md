---
description: Scan codebase and create .github/context/CODEBASE.md — project intelligence for agents
---

# Codebase Scan

## WHEN TO USE

- First-time project setup (after `/setup-agents`)
- After a large refactor that changes architecture
- When agent needs context about stack/structure but no `CODEBASE.md` yet

---

## SCAN STEPS

Two paths depending on `codegraph_mode` passed from `map-codebase`:

---

### PATH A: codegraph_mode = enabled

Use CodeGraph CLI instead of reading files manually:

| Step | CLI command | What to get |
|------|-------------|-------------|
| File structure | `codegraph files .` | Directories, languages |
| Architecture | `codegraph context "architecture overview"` | Patterns, layers |
| Entry points | `codegraph context "main entry points"` | Key symbols, call chains |
| Key symbols | `codegraph context "key classes and entry points"` | Important functions/classes |
| Index health | `codegraph status` | Files count, nodes, edges |

Also read package files (`package.json`, `build.gradle`...) to get TECH info.

---

### PATH B: codegraph_mode = disabled (manual scan)

1. **Tech Stack** — detect from file extensions and package files:
   - **Frameworks** — from dependencies
   - **Runtime** — Node / JVM / Python / .NET / Go...
   - **Build tool** — webpack, vite, gradle, maven, cargo...

---

2. **Code Structure** — read top-level folders + sample files (1-2 files per folder):

```
src/
  [folder] → detect role: api, service, model, repository, ui, util, config...
```

Identify pattern:
- **MVC** — controllers/, models/, views/
- **Clean Architecture** — domain/, application/, infrastructure/, presentation/
- **Feature-based** — features/<feature-name>/
- **Layered** — api/, service/, repo/, entity/
- **MVVM** — viewmodel/, view/, model/
- **Flat** — no clear layers

---

3. **Entry Points** — find:
- `main.kt`, `Main.java`, `app.py`, `index.ts`, `Program.cs`, `main.go`
- Routes file (routes.ts, router.kt, urls.py...)
- DI container setup (AppModule, container.ts, di.py...)

---

4. **Test Setup** — find:
- `src/test/`, `__tests__/`, `spec/`, `tests/`
- Config: `jest.config.*`, `vitest.config.*`, `pytest.ini`, `build.gradle` test block
- Patterns: unit / integration / e2e

---

5. **Key Conventions** (from existing code) — read 2-3 representative source files, identify:
- Naming convention in use
- Error handling pattern (try/catch, Result, Either...)
- Async pattern (coroutines, async/await, RxJava, Promise...)
- Comment style

---

## OUTPUT

Create file `.github/context/CODEBASE.md`. Number of sections depends on `codegraph_mode`:

```markdown
# Codebase Context
_generated: YYYY-MM-DD_
_codegraph: enabled | disabled_

---

## TECH
> Language, framework, tooling — what the project runs on

- **Language:** [primary] + [secondary if any]
- **Framework:** [...]
- **Runtime:** [Node / JVM / Python / .NET / Go / ...]
- **Build:** [webpack / vite / gradle / maven / cargo / ...]
- **Test:** [jest / vitest / pytest / junit / ...] — run: `[test command]`
- **Package manager:** [npm / yarn / pnpm / pip / gradle / ...]

---

## ARCH
> Architecture pattern, folder structure, main data flow

- **Pattern:** [MVC / Clean Architecture / Feature-based / Layered / MVVM / Flat]
- **Source root:** [src/ / app/ / lib/ / ...]
- **Entry point:** [main file, route file, DI setup]

| Folder | Role |
|--------|------|
| src/api | HTTP handlers / controllers |
| src/service | Business logic |
| src/repo | Data access |
| ... | ... |

---

## QUALITY
> Conventions used in the codebase — so agents follow the right style

- **Naming:** [camelCase / snake_case / PascalCase — where applied]
- **Error handling:** [throw / Result\<T\> / Either / return null / ...]
- **Async:** [async-await / coroutines / RxJava / Promise / callbacks]
- **Comment style:** [JSDoc / KDoc / docstring / inline / none]
- **Test pattern:** [unit / integration / e2e — coverage status]

---

## CONCERNS
> Special notes, quirks, debt, or risks agents should know

- [e.g., no test coverage for module X]
- [e.g., legacy code uses callbacks instead of async/await]
- [Leave empty if nothing notable]

---

<!-- Only present when _codegraph: enabled_ -->
## SYMBOLS
> Key symbols from codegraph index — agents use for quick lookup

| Symbol | Kind | File | Callers |
|--------|------|------|---------|
| [name] | function/class | [path] | [N] |

_Index: [N] files · [N] nodes · [N] edges (from codegraph status)_
```

---

## INTEGRATION

After creating `CODEBASE.md`:
- `setup-agents.prompt.md` STEP 1 → read this file instead of re-scanning
- Agents read `_codegraph:` flag to decide whether to use `codegraph_*` tools or grep
- Update file when architecture changes significantly
