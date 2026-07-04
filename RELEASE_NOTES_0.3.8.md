# MC-ELMA Rope 0.3.8

MC-ELMA Rope 0.3.8 strengthens world-local rope persistence coverage.

## Highlights

- Added GameTest smoke coverage for saving and restoring online player-player
  ropes.
- Added GameTest smoke coverage for saving and restoring anchored ropes.
- Added pending restore coverage for persisted links whose player endpoint is
  not online yet.
- Added persistence test cleanup helpers for state files and pending records.
- Added release jar verification for the persistence GameTest class and
  entrypoint.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` confirms the persistence GameTest entrypoint
  remains declared.
- `verifyReleaseJar` confirms the persistence GameTest class is packaged.
- The release jar is available as `mc_elma_rope-0.3.8.jar`.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
