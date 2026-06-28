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

```bash
codegraph files                            # file tree + languages
codegraph status                           # index health, file count
codegraph query "<symbol name>"            # find symbol by name
codegraph context "<task description>"     # build context for AI (callers, callees, impact)
codegraph sync .                           # incremental index update
codegraph init -i                          # initialize index for the first time
```

---

## Rules

- **Do not chain multiple `codegraph query` calls** — use `codegraph context` instead
- **Do not verify CLI results with grep** — CLI parses from AST, more accurate than grep
- **Index lag ~2s** after saving a file — do not query immediately after editing in the same turn
- If `codegraph status` reports index stale → run `codegraph sync .` first
