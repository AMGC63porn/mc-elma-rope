# MC-ELMA Rope 0.4.0-beta.2

## Changes

- Active anchored ropes now survive an orderly server shutdown and restore when
  the tied player returns after restart.
- This uses `mc_elma_rope_anchored_offline.json` and works independently of
  `persistRopes`, which remains optional for general player-player persistence.
- Existing config files gain the missing
  `persistAnchoredRopesOnDisconnect` setting automatically.
- Added automated coverage for disk-backed anchor records, shutdown capture,
  and hanging-rope tangential swing velocity.

## Compatibility

- Minecraft `1.21.10`
- Fabric Loader `0.18.4+`
- Fabric API `0.138.4+1.21.10`
- Java `21`
- Server side required
- Client side optional for rope visuals

## Server Note

Use a normal server stop so the world and anchor state can be saved. The anchor
block must still exist and remain a configured anchor when the tied player
returns.
