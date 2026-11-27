# KeepIt

KeepIt is a Kotlin Multiplatform project. Use the repository's Kotlin Toolchain wrapper. Develop behavior with TDD, keep the shared domain independent of platform APIs, and preserve KeepIt notes and reminders across database upgrades.

## Architecture

- Group files by feature and responsibility. Keep theme definitions, screen composables, form state and validation, and domain models in distinct files.
- Share domain behavior, UI, SQL schema, migrations, and repository logic across targets when supported by the current toolchain.
- Prefer maintained open-source multiplatform libraries that have been verified with KeepIt. Keep database drivers and file paths, permissions, system alarms, and notification delivery in focused platform adapters.
- Introduce common abstractions for dependencies the domain actually needs.


## Agent orchestration

- Delegate repository research to exploration agents and delivery planning to planning agents.
- Delegate implementation to agents with a model and reasoning effort calibrated to the complexity and risk of each task.
- Keep review agents independent from implementation and from one another.
- The orchestrator assigns bounded tasks, integrates concise findings, verifies important results, and intervenes when needed. Ask agents to report conclusions and evidence without returning raw logs or duplicating repository context.


## Commits and pull requests

- Keep commits focused and independently reviewable. Each commit should represent one coherent change and leave the branch buildable with relevant tests passing.
- For substantive changes, obtain three independent reviews of the same candidate before submitting the PR to the maintainer. Each reviewer operates from a distinct perspective:
  1. Product behavior, UI, and accessibility.
  2. Architecture, maintainability, and Kotlin Multiplatform compatibility.
  3. Test coverage, release reliability, and data integrity, including migrations and scheduling when relevant.
- Give each reviewer the same diff and acceptance criteria without sharing the other reviews beforehand. Resolve blocking findings and have the affected reviewers recheck material changes before submission.
- Trivial, narrowly scoped changes can be reviewed by the orchestrator or one independent reviewer. Record why the change qualifies.
- Include the applicable review summaries and verification results in the PR description. The maintainer performs the final GitHub review.
