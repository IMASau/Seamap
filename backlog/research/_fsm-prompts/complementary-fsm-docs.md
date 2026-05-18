# **Complementary Documents for the FSM Workflow**

While the **Prompt Suite** tells the LLM Orchestrator *how* to act, the system also needs structural templates and reference guides to ensure consistency across different design problems and sessions.

We should generate the following complementary documents:

## **1\. The State Board Template (00-fsm-state-template.md)**

The Orchestrator needs a blank canvas to initialize a new design problem. Instead of asking the LLM to invent the board's structure every time, providing a strict template ensures no crucial tracking data (like the cost ledger or carried-forward questions) is lost.

* **Should contain:** \* Status Line placeholder  
  * Empty Phase Table (Phases 1-9 with status checkboxes and review links)  
  * Decision / Escalation Log  
  * Cost Ledger (Budget vs. Actual per phase)  
  * Session Rules of Engagement

## **2\. The Human Handover Guide (state-based-workflow-handover.md)**

The polished, high-level overview of the workflow (which we refined earlier).

* **Purpose:** Acts as the README for human engineers or the "system prompt context" for the Orchestrator to understand *why* the rules exist.  
* **Should contain:** The 5 load-bearing pillars, the 9 phases, the anchoring playbook, and the directory layout.

## **3\. Subagent Deliverable Scaffolds (phase-templates.md)**

LLMs perform much better when given a strict markdown outline to fill in, rather than just instructions on what to write. We should generate skeleton templates for the major artefacts:

* **phase-2-understand-template.md:** Sections for Current State, Problem, Constraints, Open Questions.  
* **phase-3-specific-template.md:** Sections for Acceptance Criteria, Invariants, Escalated Mechanism Choices.  
* **phase-4-prior-art-template.md:** Sections for Source Name, Core Mechanics, Host Leakage, and "Relevance to Project".  
* **phase-5-options-template.md:** The exact Markdown table structure for the trade-off matrix.

## **4\. The Decision Record Template (phase-7-adr-template.md)**

Phase 7 (Decide) is the culmination of the FSM. It needs to produce an Architecture Decision Record (ADR) that will outlive the research dossier.

* **Purpose:** To formally record the chosen Phase 5 option and the rationale behind rejecting the others.  
* **Should contain:** Context, Considered Options, Decision, Rationale, and Consequences.

## **5\. Mock Dossier Example (example-research-dossier/)**

Not a template, but a static, completed example of a past task (e.g., the TASK-14 example mentioned in the original notes).

* **Purpose:** Gives the Orchestrator a "few-shot" learning example of what a successful end-to-end directory looks like, including how the \-v2.md amendments are structured after an adversarial review.