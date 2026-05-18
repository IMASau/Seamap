# Phase 3 — Be Specific

**Slug:** `<slug>`
**Subagent run date:** YYYY-MM-DD
**Inputs received:** Phase 2 problem statement (v-latest), carried-forward open questions.

> ⚠️ Phase 3 produces a **contract specification**. Acceptance Criteria are observable outcomes; Invariants are properties that must hold. **Mechanism choices** — anything implementable two materially different ways — are demoted to Open Questions for Phase 5, not lifted into the spec.
>
> **CRITICAL RULE:** If an AC or Output can be implemented in two materially different ways, it is *mechanism*, not contract. Lift it to an invariant outcome and explicitly demote the mechanism choice to "Open Question for Phase 5". This rule is load-bearing — Phase 5 exists to enumerate mechanism candidates against the invariants Phase 3 established. If Phase 3 picks a mechanism, Phase 5 collapses to "did we pick the right one we already picked".

## 1. Acceptance Criteria

Observable outcomes that prove the contract is satisfied. Each AC must be:

- **Observable** at a contract boundary (test, browser interaction, log output, API response, file-system state, etc.).
- **Verifiable** by a specific test or observation — name the test/observation.
- **Mechanism-free** — no commitment to *how* the system achieves the outcome.

### AC #1: <short title>

- **Observable outcome:** <what is observed>
- **Verified by:** <specific test, observation, or contract-boundary check>
- **Mechanism-free check:** Can this AC be satisfied in two materially different ways? If yes, it's wrong — lift to an invariant in §2 and demote the mechanism choice to §3.

(Add AC #2, #3, … as needed.)

## 2. Invariants

Properties that must remain true across *all* admissible implementations. Different from ACs:

- ACs are **outcomes you check** (often once, at a specific moment).
- Invariants are **properties that hold continuously** (often unobservable directly; verified via "if I tried to violate this, something else would visibly break").

### INV #1: <statement>

- **Why load-bearing:** <one line on what breaks if this is violated>
- **Verification approach:** <how do we know it's holding? Often: a test that tries to violate it and fails.>

## 3. Carried Forward / Escalated Questions

### Carried forward from Phase 2

For each question Phase 2 escalated, do exactly one of:

- **Answered in Phase 3:** <answer + evidence (file:line / observation / cite)>
- **Escalated to Phase 5+:** <reason it can't be answered now + what would unblock>

Do NOT silently drop questions.

### Mechanism choices demoted to Phase 5

For each tempting "spec it here" item that's actually mechanism, record:

- **Mechanism candidate:** <what was tempting to spec>
- **Demoted because:** <can be implemented two materially different ways: way A is …, way B is …>
- **Phase 5 candidate seeds:** <one-line list of structurally-distinct candidate directions>

## 4. Out of Scope

Things the originating brief, Phase 2 facts, or the contract might suggest belong in scope but this spec deliberately excludes. Each item one-line:

- <item> — <reason for exclusion>

## 5. Verification Plan (Pre-Test Strategy)

For each AC and INV, distinguish:

- **Automated** — what tests/checks will run; how they fail-loud if the AC is violated.
- **Manual / observational** — what must be observed by a human (e.g., browser DevTools, runtime log inspection); recorded so a future session can re-run it.

## 6. Flagged Inaccuracies / Drift From Phase 2 (if any)

If verifying Phase 2's facts in this phase reveals discrepancies, list them here. Do not silently correct.

- <fact as stated in Phase 2> → <what was actually observed in Phase 3 work>
