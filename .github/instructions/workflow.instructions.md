---
applyTo: "**"
---

# MAW — Minimal AI Workflow

Flow: **CLARIFY → THINK → DO → DONE**

## Gates (non-negotiable)
- ⛔ No code without a written Plan block
- ⛔ No DONE without review HIGH=0 + QA HIGH=0
- ⛔ Ambiguous task → ask (max 3 questions), end turn, wait for reply

## User Flags
| Flag | Action |
|------|--------|
| `--simple` | Skip CLARIFY, go straight to Plan |
| `--deep` | Read `full_analyze` skill before planning |
| `--skip-qa` | Skip QA turn (use for `docs`/`chore` only) |

## Plan format (output before any code)
```
## Plan
- Task: [summary]
- Type: feat | fix | docs | refactor | chore
- Files: [list]
- Approach: [1-3 sentences]
- Risks: [ambiguous points / "none"]
```

## CLARIFY tiers
| Tier | When | Action |
|------|------|--------|
| Clear | Outcome + scope + files all known | Skip to Plan |
| Ambiguous | Intent or scope unclear | Ask, end turn, wait |
| Complex | Multi-module, unknown root cause | Read `CODEBASE.md` → analyze skill |

## Skills
| When | File |
|------|------|
| Medium analysis | `.github/skills/analyze/analyze_lite/SKILL.md` |
| Deep analysis / `--deep` | `.github/skills/analyze/full_analyze/SKILL.md` |
| Prepare commit | `.github/skills/vcs/commit-prep/SKILL.md` |

## STATE
Read/write `.github/STATE.md` each phase. Append to `.github/logs/task-history.md` on DONE.
If file write fails → single-session mode: skip STATE writes, note "STATE unavailable" in DONE output.
