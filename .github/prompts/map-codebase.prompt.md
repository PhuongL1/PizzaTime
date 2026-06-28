---
agent: agent
---

# Map Codebase

Scan the entire codebase and update `.github/context/` with the latest project intelligence.

---

## STEP 1: CodeGraph check

**STOP, ask user:**
- If `vscode_askQuestions` is available → ask: "Use CodeGraph for scanning? (saves tokens)"
- If tool is not available → output question in chat, **END TURN, wait for reply**

**User selects NO** → `codegraph_mode = disabled` → proceed to STEP 2

**User selects YES** → check `codegraph --version` and whether `.codegraph/` exists:

| CLI | `.codegraph/` | Action |
|-----|--------------|--------|
| ✓ | ✓ | `codegraph sync .` → `codegraph_mode = enabled` → proceed to STEP 2 |
| ✓ | ✗ | `codegraph init -i` → `codegraph_mode = enabled` → proceed to STEP 2 |
| ✗ | - | Install CLI (see below) → `codegraph init -i` → `codegraph_mode = enabled` → proceed to STEP 2 |

**Install CLI (if not present):**
- Try: `npm i -g @colbymchenry/codegraph`
- If fails (network/proxy) → retry with mirror: `npm i -g @colbymchenry/codegraph --registry https://registry.npmmirror.com`
- If still fails → report error, ask "Continue with manual scan?" → YES: `codegraph_mode = disabled` / NO: stop

**`codegraph_mode` values:**
- `enabled` → use CLI: `codegraph context`, `codegraph files`, `codegraph query`
- `disabled` → use grep/read tools as normal

---

## STEP 2: Full scan

Read and follow skill: `.github/skills/codebase/codebase-scan/SKILL.md`

> Pass `codegraph_mode` to the skill — scan path differs by mode.

Output: create/replace `.github/context/CODEBASE.md`

---

## STEP 3: Dependency audit

Check if a package file exists (`package.json`, `pyproject.toml`, `build.gradle`, `go.mod`, `Cargo.toml`, `Gemfile`, `pom.xml`).

- **Found** → read and follow skill: `.github/skills/codebase/dependency-audit/SKILL.md`
  - Output: create/update `.github/context/DEPS.md`
  - If vulnerability `HIGH`/`CRITICAL` found → add to CONCERNS in `CODEBASE.md`
- **Not found** → skip this step

---

## STEP 4: Report results

```
✓ CODEBASE.md — [created / updated: sections X, Y]
✓ DEPS.md — [N vulnerabilities, M major outdated / up to date]

Context saved to .github/context/
```

If there are action items from DEPS.md → list them briefly for the human to handle.
