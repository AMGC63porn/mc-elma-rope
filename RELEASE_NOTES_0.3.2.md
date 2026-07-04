# MC-ELMA Rope 0.3.2

MC-ELMA Rope 0.3.2 improves optional visual sync cost and adds stronger build
verification for core design rules.

## Highlights

- Optional rope visual sync no longer processes active links while no ropes
  exist.
- The server sends one empty visual state after links clear so client renderers
  can remove stale ropes.
- Visual link snapshots are cached per world during each sync tick instead of
  being recomputed for every client.
- Added `verifyGameplayInvariants` to Gradle `check`.

## Guarded Invariants

- No mixin configuration.
- No teleport-based rope correction.
- Server-safe/client-optional metadata remains intact.
- Critical gameplay default values stay pinned.
- Visual sync remains optional-client aware.
- Required release and safety documents exist.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer
