# MC-ELMA Rope 0.3.4

MC-ELMA Rope 0.3.4 strengthens the anti-disconnect-abuse feature with packaged
Fabric GameTest smoke coverage.

## Highlights

- Added a GameTest that verifies tied target disconnects clear the rope, queue a
  reconnect penalty, and apply Mining Fatigue plus Slowness when the player
  returns.
- Added a GameTest that verifies controller disconnect cleanup does not punish
  the tied target.
- Added release jar verification for the disconnect GameTest class and
  entrypoint.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` confirms the disconnect GameTest entrypoint remains
  declared.
- `verifyReleaseJar` confirms the disconnect GameTest class is packaged.
- The release jar is available as `mc_elma_rope-0.3.4.jar`.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
