# MC-ELMA Rope 0.3.11

MC-ELMA Rope 0.3.11 adds repeatable headless Fabric GameTest validation and
fixes Minecraft `1.21.10` chain anchor compatibility.

## Highlights

- Added `scripts/run-headless-gametests.sh` for repeatable headless Fabric
  GameTest runs.
- Verified the full 26-test GameTest smoke suite in a headless server run.
- Switched creative/spectator checks to server game mode state.
- Added `minecraft:iron_chain` to the default anchor list while keeping
  `minecraft:chain`.

## Validation

- `./gradlew build` passes with Java 21.
- `verifyGameplayInvariants` keeps the no-mixin, no-teleport, active-link tick,
  optional-client, and gameplay-default invariants intact.
- `verifyReleaseJar` confirms metadata and required classes are packaged.
- Headless Fabric GameTest run passes with `26` tests.
- The release jar is available as `mc_elma_rope-0.3.11.jar`.
- SHA-256:
  `c301f254dd964c2b84a96949e5181d8eebc087c4c4ebb8dfd609ca19377c0f28`

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
