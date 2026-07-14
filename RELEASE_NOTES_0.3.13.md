# MC-ELMA Rope 0.3.13

## Changes

- Added anchor block break cleanup for anchored ropes.
- When the exact block used as an anchor is broken, matching anchored rope links
  are released automatically.
- Anchor break cleanup does not refund leads because it is automatic cleanup,
  not manual release.
- Added GameTest coverage that verifies broken-anchor cleanup removes only the
  matching anchor rope and leaves unrelated anchored ropes intact.

## Compatibility

- Minecraft `1.21.10`
- Fabric Loader `0.18.4+`
- Fabric API `0.138.4+1.21.10`
- Java `21`
- Server side required
- Client side optional for rope visuals

## Validation

- Run `./gradlew build`.
- Run `scripts/run-headless-gametests.sh`.
- In-game smoke test: tie a player to an anchor, break that exact anchor block,
  and confirm the rope releases without lead refund.
