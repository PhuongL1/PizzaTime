# GitHub Copilot Workflow — Setup Guide

A set of agents + prompts for GitHub Copilot following the **MAW (Minimal AI Workflow)**: every task goes through `CLARIFY → THINK → DO → DONE` with quality gates at each step.

---

## Quick Setup (< 5 minutes)

**Step 1:** Copy the `.github/` folder and `export-kit.ps1` file into your repo.

**Step 2:** Open GitHub Copilot Chat (VS Code) and run the wizards in order:

| Situation | Run order |
|-----------|-----------|
| Project already has code | `/map-codebase` → `/setup-agents` → `/setup-instructions` |
| New project | `/setup-agents` → `/setup-instructions` → `/map-codebase` (after code exists) |

**Step 3:** Reload VS Code (`Ctrl+Shift+P` → _Developer: Reload Window_).

**Step 4:** Invoke the agent:
```
@<slug>-coding Add validation to the login form
```

---

## Workflow: MAW (Minimal AI Workflow)

```
Phase 0: CLARIFY  → Classify task (Clear / Ambiguous / Complex)
                    ⛔ GATE: Ambiguous task → ask, wait for reply, then continue

Phase 1: THINK    → Output Plan block (task, type, files, approach, risks)
                    ⛔ GATE: no code without a Plan

Phase 2: DO       → Code → Review (sm-review) → QA (sm-qa)
                    ⛔ GATE: review HIGH=0 + QA HIGH=0 before proceeding

Phase 3: DONE     → DoD checklist → commit → write task-history
```

### Phase 0 — Task Classification

| Tier | When | Action |
|------|------|--------|
| **Clear** | Outcome + scope + files all known | Skip Phase 0, go to Phase 1 |
| **Ambiguous** | Intent or scope unclear | Ask up to 3 questions, wait for reply |
| **Complex** | Multiple modules, unknown root cause, architecture | Read `context/CODEBASE.md` → run analyze skill |

### User flags

Append to the end of a request to control behavior:

| Flag | Action |
|------|--------|
| _(none)_ | `--auto` default |
| `--auto` | Auto-classify tier |
| `--simple` | Skip Phase 0, go straight to Plan |
| `--deep` | Run `full_analyze` skill before planning |
| `--skip-qa` | Skip QA turn (use for `docs`/`chore` only) |

> Example: `fix button color --simple` · `refactor auth module --deep`

### Phase 2 — Gates

- **Review gate:** `sm-review` returns findings with severity HIGH/MEDIUM/LOW. Agent fixes all HIGH before QA. Max 3 rounds.
- **QA gate:** `sm-qa` tests from the user perspective (functional, edge case, regression). Max 3 rounds.

---

## Agents

### `sm-coding` — Main Orchestrator

Runs the full MAW flow. Each task:
1. Read `STATE.md` → check for in-progress tasks
2. CLARIFY → THINK (Plan block) → Code → Review → QA → DONE
3. Write `STATE.md` after each step, append `logs/task-history.md` on DONE
4. Commit after DoD passes (using `commit-prep` skill)

### `sm-review` — Code reviewer

Sub-agent called automatically by `sm-coding`. Receives `diff`, returns findings by severity. Does not propose refactors outside scope.

### `sm-qa` — QA tester

Sub-agent called after review. Tests from the user perspective: functional, edge case, error handling, regression.

---

## Wizards (Prompts)

### `/setup-agents`

Creates a set of agents for your project via adaptive Q&A:

| Question | Used for |
|----------|----------|
| Project name / slug | Naming agent files + communication |
| Tech stack | Selecting instruction template |
| Language (VI/EN) | Agent communication language |
| Test command | Integration into QA gate |
| Commit style | Format of the first commit message line |
| Conventions | Project-specific rules |

**Output:** `agents/<slug>-coding/review/qa.agent.md`, `AGENTS.md`, `STATE.md`, `logs/task-history.md`

### `/setup-instructions`

Creates an instruction file for a specific language/library. Agent auto-sets `applyTo` by file extension.

**Output:** `instructions/<lang>.instructions.md`  
**Optional:** `instructions/codegraph.instructions.md` (if `.codegraph/` is detected)

### `/map-codebase`

Scans the codebase and creates/updates `context/CODEBASE.md` — project intelligence for agents to read in Phase 0 Complex.

**Output:** `context/CODEBASE.md` with sections: TECH, ARCH, QUALITY, CONCERNS, `_codegraph:` flag

---

## Skills

Skills are detailed instruction files that agents read on demand — not auto-loaded, only read when called.

| Skill | When to use |
|-------|-------------|
| `analyze/analyze_lite` | Ambiguous task but scope < 1 day — uses 5W1H |
| `analyze/full_analyze` | Large refactor, unknown root cause, `--deep` flag |
| `codebase/codebase-scan` | Scan structure to create/update CODEBASE.md |
| `codebase/dependency-audit` | Check outdated/vulnerable deps before release |
| `vcs/commit-prep` | Format commit message per convention |
| `vcs/diff-scan` | Detect drift between code and CODEBASE.md |
| `vcs/git-revert` | Safe revert — choose strategy by situation |

---

## STATE tracking

`STATE.md` is the cross-session state file. Agent **overwrites the entire file** after each step (no patching):

```markdown
### task-20260522-1
task: "description"
phase: THINK_DONE | IN_REVIEW | IN_QA
type: feat | fix | docs | refactor | chore
plan: "1-line summary"
files: "file1, file2"
review: 1/3
```

On DONE, the task is removed from STATE and appended to `logs/task-history.md`:
```
- task-20260522-1: description → DONE 2026-05-22 | type: feat | files: ... | review: 1 | note: "..."
```

---

## File Structure

```
.github/
├── copilot-instructions.md          # Base rules (auto-loaded for every interaction)
├── README.md                        # This file
├── STATE.md                         # Task state (cross-session)
├── agents/
│   ├── sm-coding.agent.md           # Main orchestrator
│   ├── sm-review.agent.md           # Code review sub-agent
│   └── sm-qa.agent.md               # QA sub-agent
├── context/
│   └── CODEBASE.md                  # Project intelligence (generated by /map-codebase)
├── instructions/
│   ├── workflow.instructions.md     # MAW flow — applyTo:"**", loaded for Cloud Agent too
│   └── *.instructions.md            # Language/library conventions (generated)
├── logs/
│   └── task-history.md              # History of completed tasks
├── prompts/
│   ├── setup-agents.prompt.md       # Agent creation wizard
│   ├── setup-instructions.prompt.md # Instruction file creation wizard
│   └── map-codebase.prompt.md       # Codebase scan wizard
├── skills/
│   ├── analyze/analyze_lite/        # 5W1H analysis
│   ├── analyze/full_analyze/        # 3-layer deep analysis
│   ├── codebase/codebase-scan/      # Scan & generate CODEBASE.md
│   ├── codebase/dependency-audit/   # Deps check
│   ├── vcs/commit-prep/             # Commit message convention
│   ├── vcs/diff-scan/               # CODEBASE.md drift detection
│   └── vcs/git-revert/              # Safe revert strategies
└── templates/
    └── base.instructions.md         # Base template for instruction files
export-kit.ps1                       # Package kit into zip for distribution
```

---

## Distribution

Package the kit for use in another project (run from the repo root):
```powershell
.\export-kit.ps1
# Output: dist/maw-kit-YYYYMMDD.zip  (~30KB, 19 files)
```

---

## Gitignore (recommended)

```
.github/logs/task-history.md
dist/
maw-kit-*.zip
```

---

## Requirements

- VS Code + GitHub Copilot extension
- Copilot Chat with Agent Mode enabled
- Model: Claude Sonnet 4.6 (or equivalent)

---

## CodeGraph (Optional — VS Code local only)

Builds a knowledge graph from AST, helping agents find symbols/traces faster without reading files.

```bash
# Install
npm install -g @colbymchenry/codegraph

# Init in your project
cd your-project && codegraph init -i
```

Create `.vscode/mcp.json`:
```json
{
  "servers": {
    "codegraph": {
      "type": "stdio",
      "command": "codegraph",
      "args": ["serve", "--mcp"]
    }
  }
}
```

After setup, re-run `/setup-instructions` to generate `codegraph.instructions.md`. Agent automatically uses `codegraph_*` tools when `.codegraph/` exists — falls back to grep/read if not available.
