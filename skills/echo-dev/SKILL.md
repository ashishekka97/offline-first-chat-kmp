---
name: echo-dev
description: Expert developer workflow for the Echo Chat App (KMP). Use when implementing any task from ROADMAP.md to ensure adherence to SYSTEM_DESIGN.md and AGENTS.md protocols.
---

# Echo Developer Workflow

This skill provides the operational harness for developing the Echo Chat App. It forces adherence to the project's specific KMP architecture and offline-first mandates.

## Operational Workflow

When this skill is active, you MUST follow the **Implementation Protocol** defined in the root [AGENTS.md](../../AGENTS.md) file.

### Key Mandates
1.  **Bootstrap Context**: Always read `SYSTEM_DESIGN.md` and `ROADMAP.md` before starting a task.
2.  **Atomic PRs**: Limit changes to the specific files defined for the current task in the roadmap (target 2-4 files).
3.  **Offline-First Integrity**: Verify that any new data flow observes Room `Flow`s and never violates the Airplane mode requirement.
4.  **UDF Verification**: Ensure state updates follow the Unidirectional Data Flow pattern in shared ViewModels.

## Quality Gates

Before completing a task, you MUST run:
- `./gradlew :shared:build`
- `./gradlew :shared:test`
- `./gradlew :androidApp:assembleDebug`

## Troubleshooting

If build errors persist after 3 attempts, stop and report the full logs to the user as per the **Three-Strike Rule** in `AGENTS.md`.
