# Phase 2 — Understand / State

**Slug:** `<slug>`
**Subagent run date:** YYYY-MM-DD
**Inputs received:** Column A facts; carried-forward open questions.

> ⚠️ Phase 2 produces a **neutral problem statement**. No solutions, mechanisms, or fixes. If a sentence answers "how", it belongs in Phase 3 or later, not here.

## 1. Current State

What is factually happening right now. Cite files at `path:line` where applicable. No causes, no mechanisms, no fixes.

## 2. The Problem

The exact friction or missing capability — phrased as a contract failure or an absent behavior, not as an implementation gap.

State the **real outcome wanted** explicitly (from the Anchoring Filter), and distinguish it from any **symptom** that the originating brief may have used as a proxy for that outcome.

## 3. Constraints

Hard facts about the environment. Mark each item:

- **(C)** constraint — externally imposed, not negotiable in this work
- **(P)** preference — author/team taste, in principle revisitable

Examples of (C): protocol obligations, third-party API shapes, browser security model, build tool's hard-coded behavior.
Examples of (P): "we like keeping config in EDN", "prefer not to add new ports".

## 4. Open Questions

For each question carried forward, either:

- **Answer it** with evidence (file:line, command output, doc reference), OR
- **Escalate it** with a clear reason it cannot be answered here and what would unblock it.

Do NOT silently drop questions forward.

### Carried forward

1. <question> — **answered | escalated** — <evidence or escalation reason>

### Newly raised in this phase

1. <question> — <why it surfaced now>

## 5. Flagged Inaccuracies (if any)

If verifying Column A facts against the codebase reveals discrepancies, list them here. Do not silently correct upstream input.

- <fact as briefed> → <what the code actually shows, with file:line>
