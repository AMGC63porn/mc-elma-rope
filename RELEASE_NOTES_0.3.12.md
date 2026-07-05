# MC-ELMA Rope 0.3.12

MC-ELMA Rope 0.3.12 expands automated moderation and balance coverage for the
server-authoritative rope system.

## Highlights

- Added GameTest coverage for config-disabled third-party rescue.
- Added GameTest coverage for spawn-protected bind denial.
- Added GameTest coverage for protected player UUIDs.
- Added GameTest coverage for physics preset overrides.
- Added GameTest coverage for holder damage type filters.
- Added GameTest coverage for `maxHeldDurationTicks` rope expiration.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` keeps the no-mixin, no-teleport, active-link tick,
  optional-client, and gameplay-default invariants intact.
- `verifyReleaseJar` confirms metadata and required classes are packaged.
- Headless Fabric GameTest run passes with `32` tests.
- The release jar is available as `mc_elma_rope-0.3.12.jar`.
- SHA-256:
  `aec5a287448ebebdc8cb721dd68ccf43891cd14f28bc680be3bb6020488823c6`

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
