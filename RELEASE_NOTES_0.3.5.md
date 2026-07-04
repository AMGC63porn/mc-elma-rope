# MC-ELMA Rope 0.3.5

MC-ELMA Rope 0.3.5 expands the packaged Fabric GameTest smoke suite across
lifecycle cleanup and core rope physics invariants.

## Highlights

- Added controller release and anchored release permission smoke tests.
- Added dead endpoint and spectator endpoint cleanup smoke tests.
- Added a physics smoke test proving taut player-player correction changes only
  the tied player's velocity.
- Added a loose-rope physics smoke test proving in-limit ropes leave velocity
  unchanged and clear taut state.
- Added release jar verification for the lifecycle and physics GameTest classes
  and entrypoints.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` confirms lifecycle and physics GameTest
  entrypoints remain declared.
- `verifyReleaseJar` confirms the lifecycle and physics GameTest classes are
  packaged.
- The release jar is available as `mc_elma_rope-0.3.5.jar`.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
