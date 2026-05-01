## Token Usage Report

**Date**: 2026-04-30  
**Target Context Window**: 200000 tokens  
**Estimation Method**: chars ÷ 4.0  
**Flags**: none

### Assumption used for extension inventory
`.specify/extensions.yml` currently has `installed: []`, but project-local extension directories exist under `.specify/extensions/`.  
To make the report useful, I measured the **available local extensions on disk** rather than reporting extension cost as zero.

---

## Governance Files

| File | Exists | Chars | Est. Tokens | Load Timing | Notes |
|---|---:|---:|---:|---|---|
| `AGENTS.md` | Yes | 188 | 47.0 | Always | Minimal routing note |
| `.github/copilot-instructions.md` | Yes | 2062 | 515.5 | Always | Loaded by Copilot-style sessions |
| `.specify/memory/constitution.md` | Yes | 8952 | 2238.0 | Constitution-aware | Actual content, no redirect |

**Total governance tokens**: **2800.5**  
- **Baseline (always-loaded only)**: **562.5**
- **Constitution-aware load**: **2800.5**

---

## Extension Commands (ranked by token cost)

| Extension | Commands | Total Tokens | Largest Command | Largest Tokens |
|---|---:|---:|---|---:|
| `optimize` | 3 | 10311.2 | `speckit.optimize.run` | 5337.2 |
| `review` | 7 | 9175.5 | `speckit.review.errors` | 1962.5 |
| `security-review` | 7 | 8936.7 | `speckit.security-review.audit` | 4484.0 |
| `fleet` | 2 | 8853.3 | `speckit.fleet.run` | 7713.8 |
| `wireframe` | 6 | 8560.3 | `speckit.wireframe.generate` | 2269.0 |
| `ripple` | 3 | 8214.4 | `speckit.ripple.scan` | 4119.2 |
| `sync` | 5 | 5803.5 | `speckit.sync.backfill` | 1746.5 |
| `brownfield` | 4 | 5244.0 | `speckit.brownfield.migrate` | 1470.8 |
| `refine` | 4 | 3410.6 | `speckit.refine.propagate` | 1022.0 |
| `orchestrator` | 4 | 3323.8 | `speckit.orchestrator.conflicts` | 906.8 |
| `diagram` | 3 | 2734.8 | `speckit.diagram.dependencies` | 994.8 |
| `ship` | 1 | 2649.2 | `speckit.ship.run` | 2649.2 |
| `git` | 5 | 2447.1 | `speckit.git.feature` | 816.0 |
| `verify` | 1 | 2430.2 | `speckit.verify.run` | 2430.2 |
| `memory-md` | 6 | 2094.8 | `speckit.memory-md.plan-with-memory` | 513.2 |

**Largest command invocation estimate**:
- `speckit.fleet.run`
- command file: **7713.8**
- referenced docs: **1139.5**
- total invocation estimate with baseline: **9415.8**

---

## Per-Session Token Budget

| Session Type | Tokens | % of 8K | % of 32K | % of 128K | % of 200K | % of 1M |
|---|---:|---:|---:|---:|---:|---:|
| Baseline (always-loaded governance) | 562.5 | 7.03% | 1.76% | 0.44% | 0.28% | 0.06% |
| + Constitution | 2800.5 | 35.01% | 8.75% | 2.19% | 1.40% | 0.28% |
| + Largest command invocation | 9415.8 | 117.70% | 29.42% | 7.36% | 4.71% | 0.94% |

### Interpretation
- Your **baseline governance** is acceptable.
- The **constitution** is the dominant governance overhead.
- The **largest command prompts**, especially Fleet and Optimize, are expensive enough to overflow an **8K** context and consume a non-trivial share of **32K**.

---

## Historical Trend

A previous report exists at `.specify/optimize/token-report.md`.

### Comparable governance files

| File | Previous | Current | Change | Growth % | Flag |
|---|---:|---:|---:|---:|---|
| `AGENTS.md` | 47.0 | 47.0 | 0.0 | 0.0% | — |
| `.specify/memory/constitution.md` | 2328.0 | 2238.0 | -90.0 | -3.9% | shrinking |

### Important trend note
The previous report **did not count** `.github/copilot-instructions.md`, while this run **does**.

So:
- **Constitution trend** is reliable: it shrank
- **Total governance trend** is not fully apples-to-apples because discovery is more complete now

### Overall governance trend
- **Constitution**: shrinking
- **Total governance ecosystem**: effectively higher than the prior snapshot because `.github/copilot-instructions.md` is now included

---

## Optimization Suggestions

1. **Compress the biggest command prompts first**
   - Highest-impact targets:
     - `speckit.fleet.run` (~7714 tokens)
     - `speckit.optimize.run` (~5337 tokens)
     - `speckit.security-review.audit` (~4484 tokens)
     - `speckit.ripple.scan` (~4119 tokens)
   - These dominate invocation cost more than governance files do.

2. **Keep constitution work incremental**
   - You already reduced the constitution from ~2328 to ~2238 tokens.
   - Further savings are possible, but command prompt size is now the larger issue.

3. **Treat Fleet as a high-context operation**
   - `speckit.fleet.run` is too large for small-window models.
   - Prefer larger-window models for Fleet, or split orchestration into smaller commands.

4. **Use token audits periodically**
   - Now that you have a better discovery baseline, future runs will provide more reliable trend tracking.

---

## Recommended Actions

- If you want the biggest context savings, run:
  - `/speckit.optimize.run --category token_budget`
  on the **largest extension command prompts**, especially Fleet and Optimize.
- If you want command-level compression next, I’d start with:
  - `fleet`
  - `optimize`
  - `security-review`
- Governance file budget is currently **healthy** enough that extension-command optimization will likely yield better returns.
