# MC-ELMA Rope GameTest Report

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
