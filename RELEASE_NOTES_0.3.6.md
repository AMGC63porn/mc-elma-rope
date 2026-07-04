# MC-ELMA Rope 0.3.6

MC-ELMA Rope 0.3.6 expands the packaged Fabric GameTest smoke suite into timed
gameplay actions.

## Highlights

- Added a timed bind smoke test that verifies a survival player consumes one
  lead and creates a rope only after the bind duration completes.
- Added controller release and third-party rescue smoke tests, including a
  check that rescue does not finish at the faster controller duration.
- Added a tied-player abuse regression test proving tied players cannot rescue
  another player's rope.
- Added a timed anchor action smoke test that converts a carried player rope to
  an anchored rope.
- Added a default taut self-escape smoke test proving no escape action starts
  while the rope is taut.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` confirms the action GameTest entrypoint remains
  declared.
- `verifyReleaseJar` confirms the action GameTest class is packaged.
- The release jar is available as `mc_elma_rope-0.3.6.jar`.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
