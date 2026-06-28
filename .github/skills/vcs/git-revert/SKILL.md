---
description: Revert changes safely — choose the right strategy for the situation
---

# Git Revert

## 3 STRATEGIES — CHOOSE BY SITUATION

### 1. `git revert` — Safest, use when already pushed

Creates a new commit that reverses the old commit. History is preserved.

```bash
# Revert a specific commit
git revert <commit-hash>

# Revert multiple commits (newest to oldest)
git revert HEAD~3..HEAD

# Revert without committing immediately (to edit message)
git revert --no-commit <commit-hash>
git commit -m "revert(task-id): undo [description]"
```

**When to use:** Commit already pushed to remote, working on a shared branch.

---

### 2. `git reset` — More powerful, use only when not yet pushed

```bash
# Soft reset — keep changes in staging (can recommit)
git reset --soft HEAD~1

# Mixed reset (default) — keep changes in working dir, remove staging
git reset HEAD~1

# Hard reset — PERMANENTLY DISCARD changes ⚠️
git reset --hard HEAD~1
```

**When to use:** Commit is local only, not pushed. Do not use `--hard` unless certain.

---

### 3. `git checkout` / `git restore` — Revert a single file

```bash
# Restore file to state of a specific commit
git restore --source <commit-hash> path/to/file.ts

# Discard unstaged changes of a single file
git restore path/to/file.ts

# Get old version of file from commit (older git)
git checkout <commit-hash> -- path/to/file.ts
```

**When to use:** Only want to undo a single file, not the whole commit.

---

## DECISION TABLE

| Situation | Strategy |
|-----------|----------|
| Already pushed, need to undo 1 commit | `git revert <hash>` |
| Not pushed, want to undo commit but keep code | `git reset --soft HEAD~1` |
| Not pushed, want to undo commit and discard code | `git reset --hard HEAD~1` |
| Only want to undo 1 file | `git restore --source <hash> file` |
| Accidentally `git reset --hard` and want to recover | `git reflog` → find hash → `git reset --hard <hash>` |

---

## RECOVER DATA AFTER `--hard`

```bash
# git reflog stores all HEAD operations for 90 days
git reflog

# Find the line before the reset, get the hash (e.g., abc1234)
git reset --hard abc1234
```

---

## SAFETY NOTES

- **Do not `git push --force`** on shared branches — use `--force-with-lease` if absolutely required
- Before `reset --hard`: run `git stash` or note down the current hash
- `git revert` > `git reset` when in doubt — safer, reversible
