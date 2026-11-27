# KeepIt

KeepIt is a Kotlin Multiplatform project. Use the repository's Kotlin Toolchain wrapper. Develop behavior with TDD, keep the shared domain independent of platform APIs, and preserve KeepIt notes and reminders across database upgrades.

## Architecture

- Group packages and directories by feature and responsibility. Keep feature domain, storage, and presentation separate. Within presentation, put screens, components, form models, and formatting in their own packages as needed. Reusable themes belong in the shared design package.
- Share domain behavior, UI, SQL schema, migrations, and repository logic across targets when supported by the current toolchain.
- Prefer maintained open-source multiplatform libraries that have been verified with KeepIt. Keep database drivers and file paths, permissions, system alarms, and notification delivery in focused platform adapters.
- Introduce common abstractions for dependencies the domain actually needs.
- Keep development database migrations until the reminder schema is finalized. Before the first public release (0.0.1), establish one clean baseline schema for fresh installs. Preserve explicit non-destructive migrations between publicly released schemas thereafter.


## Agent orchestration

- Delegate repository research to exploration agents and delivery planning to planning agents.
- Default to the lightest capable model and low reasoning effort for bounded exploration, routine edits, and reviews. Escalate only when complexity or risk warrants it. Reserve heavyweight models and high effort for short, narrowly scoped problems where a lighter pass is insufficient.
- Keep review agents independent from implementation and from one another.
- The orchestrator assigns bounded tasks, integrates concise findings, verifies important results, and intervenes when needed. Ask agents to report conclusions and evidence without returning raw logs or duplicating repository context.
- Poll GitHub workflow jobs at most once every five minutes per run. Use the time between checks for independent work.


## Preventing repeat regressions

- Turn a reproducible defect or repeated review finding into the smallest check that would have caught it. Prefer a regression test or an executable check run by CI. Use project guidance for decisions that require judgment, and create a skill only for a repeated workflow that needs reusable instructions.
- Verify a new check against both a valid case and a representative failure. Keep checks fast and avoid duplicating an existing check.
- Run `pwsh -File scripts/check-package-layout.ps1` when changing Kotlin packages or Android component names.
- Treat Android component names used in pending intents as persistent identities across upgrades. Keep the reminder receiver entry point stable and move its implementation behind it when refactoring.


## Commits and pull requests

- Keep commits focused and independently reviewable. Each commit should represent one coherent change and leave the branch buildable with relevant tests passing.
- For substantive changes, obtain three independent reviews of the same candidate before submitting the PR to the maintainer. Each reviewer operates from a distinct perspective:
  1. Product behavior, UI, and accessibility.
  2. Architecture, maintainability, and Kotlin Multiplatform compatibility.
  3. Test coverage, release reliability, and data integrity, including migrations and scheduling when relevant.
- Give each reviewer the same diff and acceptance criteria without sharing the other reviews beforehand. Resolve blocking findings and have the affected reviewers recheck material changes before submission.
- Trivial, narrowly scoped changes can be reviewed by the orchestrator or one independent reviewer. Record why the change qualifies.
- Include the applicable review summaries and verification results in the PR description. The maintainer performs the final GitHub review.
