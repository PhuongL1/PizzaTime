# Copilot Instructions

## 1. Think Before Coding

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so — proposing a simpler implementation is not the same as adding an unrequested feature. Push back only to clarify scope or highlight specific risks that could impact the task's success.
- If something is unclear, stop. Name what's confusing. Ask.

> **Priority:** Simplicity > matching existing style > Code Quality (§6) > other guidelines. When simplicity conflicts with a §6 rule, simplicity wins for code under ~200 lines. §6 rules apply to code you **write**, not to adjacent existing code (see §3).

## 2. Simplicity First

- No features beyond what was asked. ("Push back" means raising concerns, not adding scope.)
- No abstractions for single-use code — exception: extract a helper when a function you are writing exceeds ~20 lines, even if that helper is used only once.
- §6 Code Quality rules (clean code, DRY, SOLID) apply to code **you write in this task**. Do not apply them to pre-existing code you are not changing.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

## 3. Surgical Changes

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

## 4. Goal-Driven Execution

- Define success criteria before coding
- For multi-step tasks, state a brief numbered plan with verify steps
- Ensure tests pass before and after refactors

## 5. Always Suggest Next Step

- End each response with the next logical action or command
- Be specific: concrete command, file to review, or decision to make
- Stop only when user signals completion ("finish", "ok thanks", etc.)

## 6. Code Quality Principles

### Clean Code
- Functions do one thing; name reveals intent
- No magic numbers — use named constants
- Max function length: ~20 lines; if longer, extract
- No commented-out code — delete or keep, never comment
- Boolean params are a smell — split into two functions

### DRY (Don't Repeat Yourself)
- Same logic in 2+ places → extract to shared function
- Exception: duplication across different abstraction layers is acceptable
- Don’t DRY prematurely — wait until the 3rd repetition

### SOLID
- **S** — Single Responsibility: one class/module = one reason to change
- **O** — Open/Closed: extend behavior without modifying existing code
- **L** — Liskov: subtypes must be substitutable for their base types
- **I** — Interface Segregation: no client forced to depend on methods it doesn’t use
- **D** — Dependency Inversion: depend on abstractions, not concretions

> See §1 for the global priority rule — it covers SOLID vs Simplicity conflicts.

## 7. CodeGraph — Tool Priority

This project uses the **CodeGraph CLI** (`/Users/juhalion/.local/bin/codegraph`). **Always prefer codegraph CLI over grep/read/semantic_search for structural questions. MCP tools are NOT available.**

| Question | CLI command |
|---|---|
| Where is X defined? / Find symbol named X | `codegraph query "X"` |
| What calls Y / what does Y call / impact of Z? | `codegraph context "<task description>"` |
| Project folder structure | `codegraph files` |
| Focused context for a task/area | `codegraph context "<task description>"` |
| Index health | `codegraph status` |

**Rules (non-negotiable):**
- ⛔ Do NOT grep first when looking up a symbol — use `codegraph query`.
- ⛔ Do NOT re-verify codegraph results with grep — results come from a full AST parse.
- ⛔ Do NOT chain multiple `codegraph query` calls — use `codegraph context` instead.
- Use grep/read only for **literal text** (string contents, comments, log messages) or after a file is already open.
- If `.codegraph/` doesn't exist, run `codegraph init -i` to build the index.
