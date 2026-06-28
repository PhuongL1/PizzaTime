---
description: Lightweight analysis for medium-complexity tasks — 5W1H + optional 1-2 Whys
---

# Analyze Lite

## WHEN TO USE

- Task is ambiguous but not large (scope < 1 day)
- Bug has clear symptom but unclear cause
- Feature request needs intent confirmed before planning

**Use Full (`full_analyze`) when:**
- Architecture decision, large refactor, performance optimization
- Root cause still unclear after Lite

---

## LAYER 1: 5W1H (required)

Answer all 6 in one block:

```
What:  [what feature/bug/change specifically?]
Who:   [who is affected?]
Where: [which file/module/component?]
When:  [when does it occur?]
Why:   [why is this needed?]
How:   [what is the expected behavior?]
```

If any answer is "unclear" → ask user until enough info:
- If `vscode_askQuestions` available → use that tool
- If not → output questions as numbered list, wait for reply

---

## LAYER 2: Root Cause (optional — only when Why is unclear)

Ask "Why?" up to 2 times:

```
Why [symptom]?     → [answer 1]
Why [answer 1]?    → ROOT: [answer 2]
```

If still unclear after 2 rounds → escalate to Full analyze.

---

## OUTPUT

```markdown
## Analysis
- What: [...]
- Root: [root cause / "N/A — requirement is clear"]
- Approach: [1-2 sentences]
- Risks: [remaining unknowns / "none"]
```

Attach directly to Plan block in Phase 1 THINK — no separate file needed.
