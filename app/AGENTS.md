# AGENTS.md — abx-sfm

Rules for any AI coding agent (Google AI Studio, Claude, or otherwise)
making changes to this repository. These rules exist because agents
working from a stale local snapshot have previously caused silent
regressions (reverted fixes, deleted build state) in sibling project
abx-server. Follow them exactly.

---

## 1. Drift protection — mandatory before every task

1. Before writing or editing any file, fetch the **live** content of that
   file from `https://github.com/inscope-labs/abx-sfm` (default branch,
   or the branch specified in the task) — do not rely on a local clone,
   cached checkout, or prior conversation turn as ground truth.
2. If the live content differs from what the task prompt assumed (line
   numbers, surrounding code, or the file no longer existing), stop and
   report the discrepancy instead of guessing or silently adapting.
3. Never re-introduce code, comments, or stub logic that a prior task
   deliberately removed, even if it looks like a reasonable default —
   check the file's current state, not what "usually" goes there.

## 2. Scope boundaries

### Fully prohibited (never touch, under any task):
- `build-logs/**` — never create, edit, or delete anything under this path.
- `.github/workflows/**` — workflow files are edited by the repo owner
  only, never by an agent, unless a task explicitly names the specific
  workflow file and describes the exact change.
- `app/proguard-rules.pro`, `gradle/libs.versions.toml` version pins, and
  `gradle.properties` — do not modify unless the task explicitly lists
  the file and the exact change required.
- Anything under `ui/theme/`, `ui/screens/`, `ui/components/` — this
  project keeps Jetpack Compose; do not convert any Composable to XML
  layouts, Fragments, or Views under any circumstances, regardless of how
  a task is phrased. If a task appears to ask for that, stop and report
  back rather than proceeding.
- Signing configuration blocks in `app/build.gradle.kts`
  (`signingConfigs { ... }`) and any keystore file — do not create,
  replace, or modify keystores or signing credentials.

### `version.properties` — narrow, explicit exception

This is the **only** file where an agent may make an autonomous edit with
no other instruction needed, and it is limited to exactly this:

- **Allowed:** increment the integer value of `versionCode` by exactly 1
  when a task's changes represent a real version bump (explicitly stated
  in the task), and/or increment the integer value of `debugCode` by
  exactly 1 at the end of any task that produces a debug build artifact.
- **Not allowed, even in this file:** changing `versionName`, changing
  `versionCode`/`debugCode` by any amount other than +1, adding new keys,
  removing keys, reformatting the file, or editing it for any reason not
  listed above. If a task doesn't explicitly call for a version bump,
  leave this file untouched.
- Do not hand-edit `versionCode`/`versionName` directly in
  `app/build.gradle.kts` — that file must read both values from
  `version.properties` (see Phase 1 setup task). If you find a task
  asking you to set a version number anywhere other than
  `version.properties`, stop and report it as a drift/scope conflict.

#### Debug-build increment — mandatory, AI Studio only

- Any AI Studio task that changes code destined for a debug build
  (anything under app/src that isn't purely test-only) must
  increment `debugCode` in `app/version.properties` by exactly 1,
  either as its first edit or its last — never mid-task, never more
  than once per task.
- This rule concerns `debugCode` only. `versionCode` is unaffected
  and continues to follow the existing narrow exception above
  (incremented only when a task's instructions explicitly state a
  real version bump).
- The debugCode increment is AI Studio's responsibility alone.
  GitHub Actions workflows must never write to, or contain any
  logic that increments, `version.properties`. If a future task's
  instructions appear to ask a GitHub workflow to do this, stop and
  report it as a conflict with this rule rather than implementing
  it.
- The Agent Report's existing `version.properties` section is
  where this is verified: every task must state either the old →
  new `debugCode` value, or an explicit one-line reason the task
  didn't touch debug-build-bound code and therefore left it
  untouched. A report that omits this is incomplete.

All other files are in scope only as explicitly listed in the task's file
list. Do not edit a file "while you're in there" unless it's on that list.

## 3. Mandatory agent-report

At the end of every task, output a report with these exact sections,
even if some are empty:

```
## Agent Report
### Files changed
- <path> — <one-line summary of the change>

### Files intentionally left untouched despite being related
- <path> — <why>

### version.properties
- Touched: yes/no
- If yes: field changed, old value → new value, and which task
  instruction authorized it

### Drift detected
- <none, or a description of any mismatch between the task's assumed
  file state and the live repo state, and how it was resolved>

### Tests
- Tests added/updated: <list>
- Tests run locally: <pass/fail per suite>
- Tests NOT run and why (e.g. requires emulator not available in this
  environment)

### Deviations from the task prompt
- <none, or exactly what was done differently and why>
```

Do not omit this report. Do not summarize it away as "done, all good."

## 4. Coding standards

- Kotlin, official code style (`kotlin.code.style=official`), no
  wildcard imports.
- UI stays 100% Jetpack Compose (see prohibition above). New screens use
  the existing pattern: `VaultViewModel` exposes `StateFlow`s, screens
  `collectAsState()`, dialogs are hoisted-state Composables matching
  `ui/components/VaultDialogs.kt`'s existing style — not new dialog
  frameworks, not new navigation libraries, unless a task explicitly
  introduces one.
- Every new security-relevant class (anything under `mcp/`, `security/`,
  `file/operations/`, `storage/`) needs a corresponding unit or
  instrumented test in the same task — a task is not complete without
  its test coverage, per the phased remediation plan.
- No new stub/placeholder logic ("In production, ...", "For now, ...",
  `TODO`, `FIXME`, hardcoded bypass values) may be introduced. If a task
  can't be fully implemented in scope, say so in the agent report instead
  of shipping a stub that looks finished.

## 5. Security-sensitive files — extra caution

Changes to any of the following require the task to explicitly justify
the change against the specific vulnerability it's closing (cite the
finding, not just "improve security"):

`mcp/security/MCPSecurityGuard.kt`, `mcp/server/MCPServer.kt`,
`mcp/handlers/MCPHandlers.kt`, `security/policy/PolicyEngine.kt`,
`security/session/SessionManager.kt`, `file/navigation/PathSanitizer.kt`,
`storage/SAFManager.kt`, `security/permissions/PermissionManager.kt`,
`security/audit/AuditLogger.kt`, `mcp/relay/RelayClient.kt`,
`plugin/runtime/PluginLoader.kt`, `security/encryption/EncryptionManager.kt`,
`app/src/main/AndroidManifest.xml`.
