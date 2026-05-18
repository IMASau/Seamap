# FSM State — <task-or-slug>

**Slug:** `<kebab-case-slug>`
**Originating task:** [task-NN](relative-path-to-task-file)
**Started:** YYYY-MM-DD
**Orchestrator:** <model>

## Status Line

**Current phase:** <e.g., Phase 1 (Anchoring Filter) complete — awaiting "go" for Phase 2 (Understand)>.

## Phase Table

| # | Phase | Status | Artefact | Review |
|---|---|---|---|---|
| 1 | Anchoring Filter | — | — | n/a (pre-phase) |
| 2 | Understand / State | — | — | — |
| 3 | Be Specific | — | — | — |
| 4 | Prior Art (parallel) | — | — | — |
| 5/6 | Consider & Analyse | — | — | — |
| 7 | Decide (ADR) | — | — | — |
| 8 | Implement | — | — | — |
| 9 | Verify / Close | — | — | — |

Status legend: ⏳ awaiting go · ◒ in flight · ✅ done · ⚠️ blocked.

## Restated Contract (post-Anchoring)

**Real outcome wanted:** <one-liner; the load-bearing goal after stripping anchored claims>

**Symptom(s) observed:** <what is visible — explicitly note these are proxies for the contract, not the contract itself>

## Facts (Column A, distilled)

- (Observable, citable facts only — `path:line` where applicable.)

## Open Questions Carried Forward

1. **<status: unchecked | unknown | resolved>:** <question>

## Claims Stripped (Column B)

The following originating-brief claims were **excluded** from subsequent phase briefs to prevent anchoring:

- "<claim>" — <category: assumed mechanism / scope claim / author preference / implicit goal>

## Decisions Made

- (Append at each phase boundary; date-stamp.)

## Escalations / Blockers

- (Append as they arise.)

## Cost Ledger

| Phase | Subagent | Tokens (est.) | Notes |
|---|---|---|---|
| | | | |

## Session Rules of Engagement

- No phase advances without explicit "go" from the human operator.
- Adversarial review fires after every phase before the state board is updated.
- Subagents receive Column A + carried-forward questions only; never the originating brief verbatim.
- Material feedback from adversarial review is merged into a `<artefact>-v2.md` file alongside the original.
- Mechanism choices (anything implementable two materially different ways) are demoted to Open Questions for Phase 5, not lifted into the spec.
