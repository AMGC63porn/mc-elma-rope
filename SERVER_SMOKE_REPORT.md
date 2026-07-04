# MC-ELMA Rope Server Smoke Report

## 2026-07-04 - Local Fabric Dev Server, 0.3.10

Command:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew runServer --no-daemon
```

Result: passed for boot validation.

Observed evidence:

- Minecraft `1.21.10` started with Fabric Loader `0.18.4`.
- Fabric API `0.138.4+1.21.10` loaded through the dev runtime.
- `mc_elma_rope 0.3.10` loaded on the dedicated server side.
- Server reached `Done (0.233s)! For help, type "help"`.
- The run session was terminated with `TERM` after boot validation because the
  tool session did not keep stdin open for the normal `stop` command.

Scope:

- This proves the mod boots in a local Fabric dedicated server dev runtime.
- This does not replace the full MC-ELMA modpack server validation with all
  production mods enabled.

## 2026-07-04 - Local Fabric Dev Server, 0.3.9

Command:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew runServer --no-daemon
```

Result: passed.

Observed evidence:

- Minecraft `1.21.10` started with Fabric Loader `0.18.4`.
- Fabric API `0.138.4+1.21.10` loaded through the dev runtime.
- `mc_elma_rope 0.3.9` loaded on the dedicated server side.
- Server reached `Done (...)! For help, type "help"`.
- Server accepted `stop` and shut down cleanly.

Scope:

- This proves the mod boots in a local Fabric dedicated server dev runtime.
- This does not replace the full MC-ELMA modpack server validation with all
  production mods enabled.
