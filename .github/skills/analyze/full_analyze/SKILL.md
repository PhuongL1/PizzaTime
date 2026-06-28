---
description: Full 3-layer deep analysis — use for complex tasks, unknown root cause, or --deep flag
---

# Full Analyze

## WHEN TO USE

- Architecture decision, large refactor, multiple modules affected
- Root cause completely unclear after Lite
- External library internals, complex async/memory lifecycle
- Flag `--deep` is set in the request

---

## LAYER 1: Context (5W1H) — required

Answer all 6 in one block:

```
What:  [what feature/bug/change specifically?]
Who:   [who is affected?]
Where: [which file/module/component?]
When:  [when does it occur?]
Why:   [why is this needed?]
How:   [what is the expected behavior?]
```

If any answer is "unclear" → ask user before continuing:
- If `vscode_askQuestions` is available → use that tool
- If not → numbered list in chat, end turn, wait for reply

---

## LAYER 2: Root Cause (5 Whys) — required

Ask "Why?" up to 5 times, stop when reaching a real constraint:

```
Why [symptom]?     → [answer 1]
Why [answer 1]?    → [answer 2]
Why [answer 2]?    → [answer 3]
...
ROOT CAUSE: [final answer]
```

⛔ Do not stop at Why 1-2 — those are usually symptoms, not root causes.

---

## LAYER 3: First Principles — required

1. List the **assumptions** in the current approach
2. For each assumption: ask "Is this actually necessary?"
3. Design the solution from real constraints, not from convention

```
Assumption: [...]
Truth:      [real constraint]
Conclusion: [better solution / "assumption is valid"]
```

---

## OUTPUT

```markdown
## Analysis
- What: [...]
- Root: [root cause from Layer 2]
- Assumptions challenged: [list / "none"]
- Approach: [1-3 sentences based on Layer 3]
- Risks: [remaining unknowns / "none"]
```

Attach directly to Plan block in Phase 1 THINK — no separate file needed.
