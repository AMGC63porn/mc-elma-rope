# MC-ELMA Rope 0.3.9

MC-ELMA Rope 0.3.9 adds active-link performance smoke coverage.

## Highlights

- Added GameTest smoke coverage for `0`, `50`, `100`, and `256` active rope
  links.
- Verified each active rope is ticked exactly once at the planned performance
  scales.
- Added build invariants that keep `RopeManager.tick` on an active-link list
  path with an idle fast path and without online-player scans.
- Added release jar verification for the performance GameTest class and
  entrypoint.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` confirms the performance GameTest entrypoint and
  manager tick invariants remain declared.
- `verifyReleaseJar` confirms the performance GameTest class is packaged.
- The release jar is available as `mc_elma_rope-0.3.9.jar`.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
