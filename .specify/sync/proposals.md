# Drift Resolution Proposals

Generated: 2026-04-30T19:33:26+00:00
Based on: drift-report from 2026-04-30T19:31:07+00:00

## Summary

| Resolution Type | Count |
|-----------------|-------|
| Backfill (Code → Spec) | 0 |
| Align (Spec → Code) | 10 |
| Human Decision | 1 |
| New Specs | 0 |
| Remove from Spec | 1 |

## Proposals

### Proposal 1: repo/implementation-baseline

**Direction**: HUMAN_DECISION

**Current State**:
- Spec says: "Subsystem specs, plans, and tasks describe a full MeshLink implementation, and most downstream artifacts claim migrated/complete status."
- Code does: "No product implementation tree, root build files, or project-owned CI workflows exist in the repository."

**Proposed Resolution**:

Options:
- Option A — Implementation-bearing repo: keep subsystem specs authoritative, bootstrap the actual source/build tree, and treat current not-implemented findings as execution backlog.
- Option B — Spec-only repo: keep the specs as design artifacts, but remove or clearly relabel migrated/complete plan and task statuses so they do not imply shipped code.

Questions to decide:
- Is this repository intended to host the MeshLink product implementation, or only its design/spec artifacts?
- If implementation lives elsewhere, should completed migrated plans/tasks remain here, or be archived/exported?
- If implementation should live here, what module root should be created first (`meshlink/`, `src/`, or another layout)?

**Rationale**: The absence of any product code makes all spec-to-code alignment proposals contingent on the repository’s intended role. Deciding repo mode first prevents wasted implementation or cleanup work.

**Confidence**: LOW

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 2: 001-codebase-spec-analysis/FR-001..FR-024

**Direction**: REMOVE_FROM_SPEC

**Current State**:
- Spec says: "The umbrella spec still carries 24 normative FR entries even though it now explicitly states that subsystem specs are canonical."
- Code does: "No implementation exists here to justify keeping duplicated normative ownership in the umbrella document."

**Proposed Resolution**:

Replace the detailed FR-001..FR-024 block with a compact capability summary or a traceability table that points each capability to its canonical subsystem spec. Keep end-to-end user stories in 001, but remove duplicated normative requirement ownership.

Additional updates:
- Retain the high-level user stories and success criteria that describe product-level behavior.
- Add a capability-to-subsystem mapping table instead of duplicating full subsystem FR text.

**Rationale**: This removes the last major inter-spec conflict source. The subsystem specs are already declared canonical, so keeping duplicated normative FRs in 001 increases drift risk without adding executable guidance.

**Confidence**: HIGH

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 3: babel-routing/FR-001..FR-006

**Direction**: ALIGN

**Current State**:
- Spec says: "Babel Routing defines 6 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `babel-routing` subsystem according to `specs/babel-routing/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 18/18 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 4: ble-transport/FR-001..FR-007

**Direction**: ALIGN

**Current State**:
- Spec says: "BLE Transport defines 7 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `ble-transport` subsystem according to `specs/ble-transport/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 22/22 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 5: crypto-noise-protocol/FR-001..FR-007

**Direction**: ALIGN

**Current State**:
- Spec says: "Crypto & Noise Protocol defines 7 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `crypto-noise-protocol` subsystem according to `specs/crypto-noise-protocol/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 31/31 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 6: mesh-engine/FR-001..FR-007

**Direction**: ALIGN

**Current State**:
- Spec says: "Mesh Engine defines 7 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `mesh-engine` subsystem according to `specs/mesh-engine/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 22/22 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 7: messaging-delivery/FR-001..FR-005

**Direction**: ALIGN

**Current State**:
- Spec says: "Messaging & Delivery Pipeline defines 5 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `messaging-delivery` subsystem according to `specs/messaging-delivery/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 19/19 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 8: platform-distribution/FR-001..FR-009

**Direction**: ALIGN

**Current State**:
- Spec says: "Platform & Distribution defines 9 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `platform-distribution` subsystem according to `specs/platform-distribution/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 30/30 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 9: power-management/FR-001..FR-006

**Direction**: ALIGN

**Current State**:
- Spec says: "Power Management defines 6 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `power-management` subsystem according to `specs/power-management/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 15/15 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 10: public-api/FR-001..FR-008

**Direction**: ALIGN

**Current State**:
- Spec says: "Public API & Diagnostics defines 8 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `public-api` subsystem according to `specs/public-api/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 25/25 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 11: sack-transfer/FR-001..FR-005

**Direction**: ALIGN

**Current State**:
- Spec says: "SACK Transfer defines 5 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `sack-transfer` subsystem according to `specs/sack-transfer/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 14/14 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---

### Proposal 12: wire-format/FR-001..FR-005

**Direction**: ALIGN

**Current State**:
- Spec says: "Wire Format & Codec defines 5 functional requirements and is supported by migrated planning artifacts."
- Code does: "No corresponding product implementation files are present in the repository."

**Proposed Resolution**:

Bootstrap and implement the `wire-format` subsystem according to `specs/wire-format/spec.md`, using the existing migrated plan/tasks as the starting backlog.

Implementation tasking:
- Create the source/module skeleton for this subsystem in the product implementation tree.
- Reopen or recreate the migrated tasks as executable work items unless code is imported from another repository.
- Add tests and benchmarks required by the spec success criteria before marking the subsystem complete.
- Existing artifacts: plan status = Migrated — implementation complete; tasks = 22/22 currently marked complete.

**Rationale**: The spec is the only authoritative artifact available for this subsystem. With no code present, the correct resolution is to align the repository to the approved design rather than weaken the subsystem requirements.

**Confidence**: MEDIUM

**Action**:
- [ ] Approve
- [ ] Reject
- [ ] Modify

---
