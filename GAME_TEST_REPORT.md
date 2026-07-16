# MC-ELMA Rope GameTest Report

## 2026-07-16 - Headless Fabric GameTest, 0.4.0-beta.2 anchor restart fix

Command:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
scripts/run-headless-gametests.sh
```

Result: passed.

Observed evidence:

- Minecraft `1.21.10` started with Fabric Loader `0.18.4`.
- Fabric API `0.138.4+1.21.10` loaded through the dev runtime.
- `mc_elma_rope 0.4.0-beta.2` loaded on the dedicated server side.
- Fabric GameTest headless server started.
- `39` tests ran.
- Added disk reload coverage for anchored disconnect records.
- Added orderly server shutdown capture coverage for active anchored ropes.
- Added hanging-anchor tangential velocity coverage for rope swing behavior.
- The Gradle process exited successfully.
- XML report:
  `build/gametest-results/mc_elma_rope-gametest.xml`.

Scope:

- This proves the anchor persistence and physics smoke suite runs in a real
  headless Fabric server environment.
- This does not replace full MC-ELMA modpack multiplayer validation with all
  production mods enabled.

## 2026-07-14 - Headless Fabric GameTest, 0.4.0-beta.1 preparation

Command:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
scripts/run-headless-gametests.sh
```

Result: passed.

Observed evidence:

- Minecraft `1.21.10` started with Fabric Loader `0.18.4`.
- Fabric API `0.138.4+1.21.10` loaded through the dev runtime.
- `mc_elma_rope 0.4.0-beta.1` loaded on the dedicated server side.
- Fabric GameTest headless server started.
- `36` tests ran.
- New anchored disconnect coverage was included:
  `rope_disconnect_game_tests_anchored_target_disconnect_persists_without_lead_refund_and_restores`.
- New offline anchor cleanup coverage was included:
  `rope_disconnect_game_tests_broken_anchor_clears_offline_anchored_record`.
- New invalid offline anchor coverage was included:
  `rope_disconnect_game_tests_invalid_offline_anchor_does_not_restore`.
- The Gradle process exited successfully.
- XML report:
  `build/gametest-results/mc_elma_rope-gametest.xml`.

Scope:

- This proves the Fabric GameTest smoke suite runs in a real headless server
  test environment.
- This does not replace full MC-ELMA modpack multiplayer validation with all
  production mods enabled.

## 2026-07-11 - Headless Fabric GameTest, 0.3.13 preparation

Command:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
scripts/run-headless-gametests.sh
```

Result: passed.

Observed evidence:

- Minecraft `1.21.10` started with Fabric Loader `0.18.4`.
- Fabric API `0.138.4+1.21.10` loaded through the dev runtime.
- `mc_elma_rope 0.3.13` loaded on the dedicated server side.
- Fabric GameTest headless server started.
- `33` tests ran.
- Broken-anchor cleanup coverage was included:
  `rope_lifecycle_game_tests_broken_anchor_clears_only_matching_anchored_rope`.
- The Gradle process exited successfully.
- XML report:
  `build/gametest-results/mc_elma_rope-gametest.xml`.

Scope:

- This proves the Fabric GameTest smoke suite runs in a real headless server
  test environment.
- This does not replace full MC-ELMA modpack multiplayer validation with all
  production mods enabled.

## 2026-07-05 - Headless Fabric GameTest, 0.3.12 preparation

Command:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
scripts/run-headless-gametests.sh
```

Result: passed.

Observed evidence:

- Minecraft `1.21.10` started with Fabric Loader `0.18.4`.
- Fabric API `0.138.4+1.21.10` loaded through the dev runtime.
- `mc_elma_rope` loaded on the dedicated server side.
- Fabric GameTest headless server started.
- `32` tests ran.
- The Gradle process exited successfully.
- XML report:
  `build/gametest-results/mc_elma_rope-gametest.xml`.

Scope:

- This proves the Fabric GameTest smoke suite runs in a real headless server
  test environment.
- This does not replace full MC-ELMA modpack multiplayer validation with all
  production mods enabled.

## 2026-07-04 - Headless Fabric GameTest, 0.3.11 preparation

Command:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
JAVA_TOOL_OPTIONS="-Dfabric-api.gametest=true -Dfabric-api.gametest.report-file=/private/tmp/mc_elma_rope-gametest.xml" \
./gradlew runServer --no-daemon
```

Result: passed.

Observed evidence:

- Minecraft `1.21.10` started with Fabric Loader `0.18.4`.
- Fabric API `0.138.4+1.21.10` loaded through the dev runtime.
- `mc_elma_rope` loaded on the dedicated server side.
- Fabric GameTest headless server started.
- `26` tests ran.
- The Gradle process exited successfully.
- XML report: `/private/tmp/mc_elma_rope-gametest.xml` during the manual run;
  `scripts/run-headless-gametests.sh` copies future reports to
  `build/gametest-results/mc_elma_rope-gametest.xml`.

Scope:

- This proves the Fabric GameTest smoke suite runs in a real headless server
  test environment.
- This does not replace full MC-ELMA modpack multiplayer validation with all
  production mods enabled.
