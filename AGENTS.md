# KeepIt

KeepIt is a Kotlin Multiplatform project. Use the repository's Kotlin Toolchain wrapper. Develop behavior with TDD, keep the shared domain independent of platform APIs, and preserve KeepIt notes and reminders across database upgrades.

## Agent orchestration

- Delegate repository research to exploration agents and delivery planning to planning agents.
- Delegate implementation to agents with a model and reasoning effort calibrated to the complexity and risk of each task.
- Keep review agents independent from implementation and from one another.
- The orchestrator assigns bounded tasks, integrates concise findings, verifies important results, and intervenes when needed. Ask agents to report conclusions and evidence without returning raw logs or duplicating repository context.


## Commits and pull requests

- Keep commits focused and independently reviewable. Each commit should represent one coherent change and leave the branch buildable with relevant tests passing.
- Before submitting a PR to the maintainer, obtain three independent reviews of the same candidate, each from a different reviewer operating from a distinct perspective:
  1. Product behavior, UI, and accessibility.
  2. Architecture, maintainability, and Kotlin Multiplatform compatibility.
  3. Test coverage, release reliability, and data integrity, including migrations and scheduling when relevant.
- Give each reviewer the same diff and acceptance criteria without sharing the other reviews beforehand. Resolve blocking findings and have the affected reviewers recheck material changes before submission.
- Include the three review summaries and verification results in the PR description. The maintainer performs the final GitHub review.
