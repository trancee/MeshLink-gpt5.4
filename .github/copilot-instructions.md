# Copilot Instructions

This repository uses the Spec Kit memory workflow. For non-trivial tasks, memory is required.

## Memory Model

- Read `.specify/memory/constitution.md` first.
- Durable project memory lives in `docs/memory/`.
- Active feature memory lives in `specs/<feature>/memory.md` and `specs/<feature>/memory-synthesis.md`.
- Ephemeral run context belongs in the prompt, diff, terminal output, and temporary notes only; do not commit it to durable memory.

## Required Workflow

- Before `/specify`: read the constitution, relevant durable memory, and related bug or decision entries. Produce or refresh `memory-synthesis.md` with constraints, reused decisions, boundaries, conflicts, assumptions, and watchpoints.
- Before `/plan` and `/tasks`: read the active spec plus feature memory. Do not proceed through unresolved conflicts with project memory or architecture boundaries.
- Before `/implement`: re-read `memory-synthesis.md` and treat implementation and verification watchpoints as requirements.
- After `/implement` and `/verify`: review the diff, completed work, tests, and findings. Update durable memory only when the lesson is durable, evidenced, reusable, and non-obvious. Do not add changelog-style or speculative entries.

Treat `docs/memory/` as repository memory. Keep entries concise, durable, and reviewable in Git.

A task is not complete until memory has been reviewed.
