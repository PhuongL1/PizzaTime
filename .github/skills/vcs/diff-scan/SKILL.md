---
description: Detect codebase changes vs current CODEBASE.md and update affected sections
---

# Diff Scan

## WHEN TO USE

- After a refactor that changes architecture or folder structure
- After adding/removing a framework or major library
- When CODEBASE.md feels outdated compared to current code
- **Not needed** after every small feature — only when structure/stack changes

---

## SCAN STEPS

### 1. Read current CODEBASE.md

Read `.github/context/CODEBASE.md`. If it doesn't exist → run `codebase-scan` instead.

Note:
- `_generated:` date
- Values in each section (TECH / ARCH / QUALITY / CONCERNS)

---

### 2. Detect changes per section

**TECH drift** — check if dependencies changed:
```
git diff HEAD~20 -- package.json build.gradle pyproject.toml go.mod Cargo.toml *.csproj
```
Or compare the current file with values recorded in CODEBASE.md.

Signs that TECH needs updating:
- New framework appears in dependencies
- Build tool changed (e.g., webpack → vite)
- New language added (e.g., TypeScript into a JS project)

---

**ARCH drift** — check if folder structure changed:
```
git diff HEAD~20 --name-only --diff-filter=A | grep "/" | cut -d/ -f1,2 | sort -u
```
Or list current top-level folders and compare with the Folder/Role table in CODEBASE.md.

Signs that ARCH needs updating:
- New folder appears not in the table
- Old folder deleted
- Pattern detected differently (e.g., added `features/` → shifted from Layered to Feature-based)

---

**QUALITY drift** — check 2-3 most recent source files:
```
git diff HEAD~10 --name-only -- "*.ts" "*.kt" "*.py" "*.go" | head -5
```
Read those files and compare conventions with the QUALITY section.

Signs that QUALITY needs updating:
- Naming convention changed
- New error handling pattern (e.g., started using Result<T> instead of throw)
- New async pattern adopted

---

**CONCERNS** — manual review:
- Read current CONCERNS
- Check if old items are still relevant (e.g., "legacy callback" — has it been refactored?)
- Any new debt/quirks to add?

---

### 3. Decide action

| Result | Action |
|--------|--------|
| No drift | Report "CODEBASE.md up to date" — update `_generated:` date |
| Drift in 1-2 sections | Update only affected sections |
| Drift everywhere or large structure change | Re-run `codebase-scan` to regenerate completely |

---

## OUTPUT

- Update **in-place** the affected sections in `.github/context/CODEBASE.md`
- Update `_generated: YYYY-MM-DD` to today's date
- Summarize what changed: `Updated sections: TECH (added Vite), ARCH (added features/ folder)`
- If nothing changed: `CODEBASE.md current as of YYYY-MM-DD — no drift detected`
