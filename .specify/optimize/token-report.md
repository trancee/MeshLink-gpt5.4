## Token Usage Report

**Date**: 2026-05-01  
**Target Context Window**: 200000 tokens  
**Estimation Method**: chars ÷ 4.0  
**Installed extensions counted**: from `.specify/extensions.yml` only

### Scope note
A previous report counted **local extension directories on disk** even though they were not installed.  
This run follows the current command spec strictly and counts **only installed extensions**.

---

## Governance Files

| File | Exists | Chars | Est. Tokens | Load Timing | Notes |
|---|---|---:|---:|---|---|
| `CLAUDE.md` | No | 0 | 0.0 | Always | Not present |
| `AGENTS.md` | Yes | 188 | 47.0 | Always | Minimal routing note; references “current plan” without concrete path |
| `.github/copilot-instructions.md` | Yes | 2062 | 515.5 | Always | References constitution + durable memory files |
| `.specify/memory/constitution.md` | Yes | 8952 | 2238.0 | Constitution-aware | Actual content, no redirect |
| `docs/memory/ARCHITECTURE.md` | Yes | 653 | 163.2 | Referenced by always-loaded file | Referenced from `.github/copilot-instructions.md` |
| `docs/memory/BUGS.md` | Yes | 515 | 128.8 | Referenced by always-loaded file | Referenced from `.github/copilot-instructions.md` |
| `docs/memory/DECISIONS.md` | Yes | 585 | 146.2 | Referenced by always-loaded file | Referenced from `.github/copilot-instructions.md` |
| `docs/memory/PROJECT_CONTEXT.md` | Yes | 657 | 164.2 | Referenced by always-loaded file | Referenced from `.github/copilot-instructions.md` |
| `docs/memory/WORKLOG.md` | Yes | 218 | 54.5 | Referenced by always-loaded file | Referenced from `.github/copilot-instructions.md` |

**Total governance tokens**: **3457.4**  
**Baseline only**: **562.5**  
**Supplementary governance beyond constitution**: **656.9**

### Governance composition
- **Constitution**: 2238.0 tokens (**64.7%** of constitution-aware load)
- **Copilot instructions**: 515.5 tokens (**14.9%**)
- **Referenced durable memory files**: 656.9 tokens (**19.0%**)
- **AGENTS.md**: 47.0 tokens (**1.4%**)

### Unresolved / template references
These were detected but not counted as concrete files:
- `AGENTS.md` → “current plan” (textual reference, no path)
- `.github/copilot-instructions.md` → `specs/<feature>/memory.md`
- `.github/copilot-instructions.md` → `specs/<feature>/memory-synthesis.md`

No extra files found under:
- `.ai/rules/*.md`
- `.specify/memory/*.md` beyond `constitution.md`

---

## Extension Commands (ranked by token cost)

| Extension | Commands | Total Tokens | Largest Command | Largest Tokens |
|---|---:|---:|---|---:|
| *(none installed in `.specify/extensions.yml`)* | 0 | 0.0 | — | — |

**Total extension tokens**: **0.0**

### Important note
Project-local extension directories do exist under `.specify/extensions/`, but they are **not counted** here because they are **not installed** in `.specify/extensions.yml`.

---

## Per-Session Token Budget

| Session Type | Tokens | % of 8K | % of 32K | % of 128K | % of 200K | % of 1M |
|---|---:|---:|---:|---:|---:|---:|
| Baseline (governance only) | 562.5 | 7.03% | 1.76% | 0.44% | 0.28% | 0.06% |
| + Constitution + referenced durable memory | 3457.4 | 43.22% | 10.80% | 2.70% | 1.73% | 0.35% |
| + Largest installed command | — | — | — | — | — | — |

### Interpretation
- **200K / 128K / 32K models**: governance overhead is healthy.
- **8K models**: a constitution-aware session is expensive at **43.22%** before task-specific context.
- **Installed extension prompt overhead is currently zero**.

---

## Historical Trend

Previous report found at:
- `.specify/optimize/token-report.md`

### Per-file comparison

| File | Previous | Current | Change | Growth % | Flag |
|---|---:|---:|---:|---:|---|
| `.github/copilot-instructions.md` | 515.5 | 515.5 | 0.0 | 0.0% | — |
| `.specify/memory/constitution.md` | 2238.0 | 2238.0 | 0.0 | 0.0% | — |
| `AGENTS.md` | 47.0 | 47.0 | 0.0 | 0.0% | — |
| `docs/memory/ARCHITECTURE.md` | — | 163.2 | — | — | new scope |
| `docs/memory/BUGS.md` | — | 128.8 | — | — | new scope |
| `docs/memory/DECISIONS.md` | — | 146.2 | — | — | new scope |
| `docs/memory/PROJECT_CONTEXT.md` | — | 164.2 | — | — | new scope |
| `docs/memory/WORKLOG.md` | — | 54.5 | — | — | new scope |

**Growth threshold**: 20%  
**Files over threshold**: none

### Overall governance trend
- **Comparable tracked files**: **Stable (0.0%)**
- **Measured total governance overhead** is higher than the previous report because this run includes additional concrete files referenced by `.github/copilot-instructions.md`.

### Extension trend note
The previous report counted **local extensions on disk** despite `installed: []`.  
This run counts **installed extensions only**, so **extension totals are not directly comparable**.

---

## Optimization Suggestions

Suggest-only; nothing applied.

1. **Keep the constitution purely normative**
   - Move sync-impact/history commentary out of `.specify/memory/constitution.md` into a companion doc.
   - Projected savings: **~80–150 tokens**
   - Why: the constitution is your biggest active governance file by far.

2. **Compress `.github/copilot-instructions.md` into a tighter checklist**
   - Replace repeated phase prose with a shorter decision checklist plus one pointer to durable memory guidance.
   - Projected savings: **~100–180 tokens**
   - Why: this is always-loaded overhead.

3. **Introduce a single durable-memory index**
   - Example: `docs/memory/README.md`, then reference that from `.github/copilot-instructions.md` instead of listing five files inline.
   - Projected savings: **~50–100 baseline tokens**
   - Why: reduces always-loaded prompt size, even if the deeper docs remain available when needed.

4. **Treat 8K models as “baseline-only” unless necessary**
   - Savings: **context preservation rather than file reduction**
   - Why: loading constitution + durable memory consumes **43.22%** of an 8K window before task context.

5. **If extension activation is planned, audit before installing**
   - Current installed cost: **0**
   - Prevented future cost: **potentially thousands of tokens**
   - Why: the previous report showed large local command prompts, but they are inactive today.

---

## Recommended Actions

- If you mainly use **32K+ models**, your current governance budget is fine.
- If you want to support **8K models**, compress:
  - `.specify/memory/constitution.md`
  - `.github/copilot-instructions.md`
- If `installed: []` is intentional, extension-token optimization is **not urgent**.
- If you plan to activate extensions later, update `.specify/extensions.yml` and rerun this audit first.
- Run this audit periodically to keep governance growth visible.
