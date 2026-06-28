---
description: Prepare commit message following convention — type(task-id): desc + body
---

# Commit Prep

## FORMAT

```
type(task-id): short description (≤72 characters)

files: file1, file2, ...
review: N rounds
note: "key decision or important context"
```

---

## TYPES

| Type | When to use |
|------|-------------|
| `feat` | Add a new feature |
| `fix` | Fix a bug |
| `refactor` | Restructure code without adding/changing behavior |
| `docs` | Documentation, README, comments only |
| `chore` | Config, tooling, dependency — no logic impact |
| `test` | Add or update tests |
| `style` | Formatting, whitespace — no logic change |

---

## RULES

- **1 task = 1 commit** — do not combine multiple tasks in one commit
- `task-id` is required — format: `task-YYYYMMDD-N` (e.g., `task-20260520-3`)
- First line ≤72 characters — git log displays cleanly
- `note:` only when there is an important decision or quirk to know — leave blank otherwise
- Do not use generic messages: ~~"update code"~~, ~~"fix bug"~~, ~~"WIP"~~

---

## CHECKLIST BEFORE COMMIT

```
[ ] git status — only stage files belonging to this task
[ ] No debug code / console.log / leftover TODOs
[ ] Type chosen correctly (feat/fix/refactor/docs/chore/test)
[ ] task-id matches STATE.md
[ ] First line ≤72 characters
```

---

## EXAMPLES

```bash
# Correct
git commit -m "feat(task-20260520-3): add raycasting interaction for shapes

files: index.html
review: 2 rounds
note: \"mouseup on window prevents stuck orbit after drag outside canvas\""

# Incorrect
git commit -m "update index.html"
git commit -m "feat: fix bug"   # type contradiction
git commit -m "task-3: add stuff"   # missing type
```

---

## WHEN TO USE

Agent reads this skill when:
- Preparing a commit in Phase 3 DONE
- Need to verify message format before pushing
- Human asks "what type should this commit use?"
