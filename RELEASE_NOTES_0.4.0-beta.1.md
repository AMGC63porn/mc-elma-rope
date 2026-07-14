# MC-ELMA Rope 0.4.0-beta.1

## Changes

- Replaced the default three-line rope visual with a vanilla-like procedural
  braided rope renderer.
- Added `ropeVisualStyle`, defaulting to `vanilla_like`; `layered_lines` keeps
  the previous renderer available.
- Added `persistAnchoredRopesOnDisconnect`, enabled by default.
- Anchored tied players who disconnect are stored in offline anchored rope
  state instead of receiving a lead refund.
- Anchored tied players restore to the same anchor when they rejoin in the
  anchor world and the anchor block is still valid.
- Existing disconnect penalties still apply to anchored tied players who leave
  while tied.
- Breaking an anchor block now removes both active anchored ropes and offline
  anchored rope records.

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
- In-game smoke test: anchor a tied player, disconnect/reconnect that player,
  and confirm they are still tied to the same anchor.
- Client smoke test: install the mod client-side and confirm the rope uses the
  vanilla-like visual style instead of the old three-line look.
