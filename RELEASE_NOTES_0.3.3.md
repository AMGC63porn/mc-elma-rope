# MC-ELMA Rope 0.3.3

MC-ELMA Rope 0.3.3 adds packaged Fabric GameTest smoke coverage and extends the
release verification gates around that coverage.

## Highlights

- Added Fabric GameTest smoke tests for core config defaults.
- Added smoke coverage for default fence and chain anchor support.
- Added manager tests for self-link rejection, anchor cleanup, length clamping,
  and persisted restore duplicate protection.
- Added release jar verification for the GameTest class and entrypoint.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` confirms the GameTest entrypoint remains declared.
- `verifyReleaseJar` confirms the smoke test class is packaged.
- The release jar is available as `mc_elma_rope-0.3.3.jar`.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
