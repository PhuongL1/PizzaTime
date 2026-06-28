---
description: Check outdated and vulnerable dependencies — per project package manager
---

# Dependency Audit

## WHEN TO USE

- Before release / creating a large PR
- When CODEBASE.md CONCERNS has a note about outdated deps
- Periodically (every milestone)

---

## DETECT PACKAGE MANAGER

Read CODEBASE.md TECH section or check files:

| File present | Package manager |
|---|---|
| `package.json` | npm / yarn / pnpm |
| `pyproject.toml` / `requirements.txt` | pip / poetry |
| `build.gradle` / `build.gradle.kts` | Gradle |
| `pom.xml` | Maven |
| `go.mod` | Go modules |
| `Cargo.toml` | Cargo (Rust) |
| `Gemfile` | Bundler (Ruby) |

---

## AUDIT COMMANDS

### Node.js (npm / yarn / pnpm)
```bash
npm audit                    # security vulnerabilities
npm outdated                 # outdated packages
npx npm-check-updates        # show available upgrades
```

### Python
```bash
pip list --outdated          # outdated packages
pip audit                    # vulnerabilities (pip-audit package)
safety check                 # alternative security scanner
```

### Gradle (Android / JVM)
```bash
./gradlew dependencyUpdates  # requires plugin com.github.ben-manes.versions
./gradlew dependencies        # full dependency tree
```

### Maven
```bash
mvn versions:display-dependency-updates
mvn dependency:tree
```

### Go
```bash
go list -u -m all            # outdated modules
govulncheck ./...             # vulnerabilities
```

### Cargo (Rust)
```bash
cargo outdated               # outdated crates
cargo audit                  # vulnerabilities
```

---

## OUTPUT FORMAT

Write results to `.github/context/DEPS.md`:

```markdown
# Dependency Audit
_generated: YYYY-MM-DD_

## Vulnerabilities
| Package | Severity | CVE | Fix |
|---------|----------|-----|-----|
| lodash  | HIGH     | CVE-2021-xxxx | upgrade to 4.17.21 |

## Outdated (major bumps only)
| Package | Current | Latest | Breaking? |
|---------|---------|--------|-----------|
| react   | 18.x    | 19.x   | yes — check migration guide |

## Action Items
- [ ] [package]: upgrade from X → Y (security fix)
- [ ] [package]: evaluate major upgrade
```

If no vulnerabilities and no major outdated packages → write: `All dependencies up to date as of YYYY-MM-DD`

---

## NOTES

- Only flag **major** outdated — patch/minor don't need immediate action
- Severity `HIGH` / `CRITICAL` → add to CONCERNS in CODEBASE.md
- Do not upgrade automatically — report only, let human decide
