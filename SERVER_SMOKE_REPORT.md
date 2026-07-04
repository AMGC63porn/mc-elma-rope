# MC-ELMA Rope Server Smoke Report

## 2026-07-04 - Local Fabric Dev Server

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
