# MC-ELMA Rope 0.3.10

MC-ELMA Rope 0.3.10 strengthens disconnect-abuse regression coverage.

## Highlights

- Added GameTest coverage proving that a tied target disconnect can refund one
  lead to the survival-mode controller.
- Added a regression check that command-created ropes do not queue reconnect
  penalties by default.
- Kept the release focused on verification hardening; gameplay defaults remain
  unchanged from `0.3.9`.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` keeps the no-mixin, no-teleport, active-link tick,
  optional-client, and gameplay-default invariants intact.
- `verifyReleaseJar` confirms the disconnect GameTest class is packaged.
- The release jar is available as `mc_elma_rope-0.3.10.jar`.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
