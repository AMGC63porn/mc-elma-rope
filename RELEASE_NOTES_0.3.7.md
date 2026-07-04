# MC-ELMA Rope 0.3.7

MC-ELMA Rope 0.3.7 strengthens config reload and fallback coverage.

## Highlights

- Reused the same config install path for file loads and tests so normalization
  and anchor refresh behavior stay consistent.
- Added GameTest smoke coverage for invalid config value sanitization.
- Added custom anchor id and block tag reload coverage.
- Added malformed config fallback coverage.
- Added protected player name reload coverage.
- Added release jar verification for the config GameTest class and entrypoint.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` confirms the config GameTest entrypoint remains
  declared.
- `verifyReleaseJar` confirms the config GameTest class is packaged.
- The release jar is available as `mc_elma_rope-0.3.7.jar`.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
