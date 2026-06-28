---
agent: agent
description: "Instruction wizard: asks for tech stack + libraries → generates .github/instructions/*.instructions.md"
---

# Setup Instructions — Convention Wizard

Create or update instruction files for the project. Can be run independently at any time (no need to re-run setup-agents).

Follow the sequence exactly: **SCAN → CONFIRM → Q&A → GENERATE**

---

## STEP 1: SCAN

Read context sources in priority order:

1. `.github/context/CODEBASE.md` — if present, use as primary source
2. `.github/instructions/` — list existing files (to know whether to update or create)
3. Config files: `build.gradle`, `package.json`, `pyproject.toml`, `go.mod`, `*.csproj`

Infer from scan:
- `detected_languages`: list of languages (e.g., Kotlin, XML, Python)
- `existing_instructions`: list of files already in `.github/instructions/`

---

## STEP 2: CONFIRM

Ask user to confirm the languages/stack to generate instructions for:

```
header: "Languages"
question: "Languages / stack to generate instructions for? (can list multiple, e.g., 'Kotlin, XML')"
→ Pre-fill with detected_languages if available
→ Required — user must provide
```

If `existing_instructions` is not empty, also ask:
```
header: "Update Mode"
question: "Existing files: [list]. Overwrite or skip?"
options: ["Overwrite all", "Create new files only", "Choose per file"]
```

---

## STEP 3: Q&A per language

For **each language** the user listed, ask 2 questions:

**Question 1 — Libraries:**
```
header: "[Lang] Libraries"
question: "Which libraries/frameworks does the project use for [lang]?
           (free-form list, e.g., 'Retrofit, Hilt, Room, Coil')"
→ Empty = standard library only
```

**Question 2 — Special conventions:**
```
header: "[Lang] Conventions"
question: "Any special conventions beyond standard?
           (e.g., 'use MVI pattern', 'no coroutines, use RxJava')"
→ Empty = nothing special
```

**Fallback (no `vscode_askQuestions`):**
- Output questions as numbered list per language
- **END TURN** — wait for user reply then proceed to STEP 4

---

## STEP 4: GENERATE instruction files

For each language, create `.github/instructions/<lang>.instructions.md`:

### Content sources (in priority order):

1. **Template exists** in `.github/templates/instructions/` → read template as base
2. **No template** → use `_base.instructions.md` as scaffold, fill in appropriate sections

### Then append Libraries section (if user provided):

```markdown
## Libraries

<!-- Conventions below are based on libraries used in the project -->

### <LibraryName>
- [Convention 1]
- [Convention 2]
- [Convention 3]
```

**Important:** Use the model's internal knowledge to generate conventions for each library — no lookups, no hardcoding. Conventions must be:
- Specific and actionable (not generic like "follow best practices")
- Appropriate for the current popular version of that library
- Include: initialization, usage patterns, anti-patterns to avoid

### applyTo per language:

| Language | applyTo |
|----------|---------|
| Kotlin | `**/*.kt` |
| Java | `**/*.java` |
| Python | `**/*.py` |
| TypeScript | `**/*.ts, **/*.tsx` |
| JavaScript | `**/*.js, **/*.jsx` |
| Swift | `**/*.swift` |
| Go | `**/*.go` |
| C# | `**/*.cs` |
| XML (Android) | `**/res/**/*.xml` |
| Dart/Flutter | `**/*.dart` |
| Ruby | `**/*.rb` |
| Rust | `**/*.rs` |
| Unknown | `**` |

---

---

## STEP 5: CodeGraph (Optional)

Check if `.codegraph/` exists in the project:

**If present** → create the following file:
```markdown
---
applyTo: "**"
---

# CodeGraph CLI Instructions

## Availability check

Before using any CodeGraph command:
1. Check that `.codegraph/` exists in the project root
2. Check that `codegraph --version` succeeds

If either check fails → **do not use CodeGraph**, fall back to grep/read tools.

---

## When to use CodeGraph CLI

Use CLI for **structural questions** — where a symbol is defined, what calls what, impact of a change:

| Question | CLI command |
|---------|-------------|
| "Where is X defined?" / "Find symbol X" | `codegraph query "X"` |
| "What calls Y / what does Y call / impact of changing Z?" | `codegraph context "<task description>"` |
| "Project folder structure" | `codegraph files` |
| "Context for this task" | `codegraph context "<task description>"` |
| "Is the index healthy?" | `codegraph status` |

Use grep/read for **literal questions** — string content, comments, log messages.

---

## CLI reference

\`\`\`bash
codegraph files                            # file tree + languages
codegraph status                           # index health, file count
codegraph query "<symbol name>"            # find symbol by name
codegraph context "<task description>"     # build context for AI (callers, callees, impact)
codegraph sync .                           # incremental index update
codegraph init -i                          # initialize index for the first time
\`\`\`

---

## Rules

- **Do not chain multiple `codegraph query` calls** — use `codegraph context` instead
- **Do not verify CLI results with grep** — CLI parses from AST, more accurate than grep
- **Index lag ~2s** after saving a file — do not query immediately after editing in the same turn
- If `codegraph status` reports index stale → run `codegraph sync .` first
```

**If `.codegraph/` is absent** → skip, create no files.

---

## STEP 6: Report results

```
✓ Setup Instructions complete!

Generated/Updated:
- .github/instructions/<lang1>.instructions.md
- .github/instructions/<lang2>.instructions.md
- .github/instructions/codegraph.instructions.md (if .codegraph/ exists)

Next: Reload VS Code (Cmd+Shift+P → "Developer: Reload Window")
so instructions are applied automatically when editing files.
```
