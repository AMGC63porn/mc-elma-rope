# MC-ELMA Rope 0.3.0

MC-ELMA Rope 0.3.0 focuses on server safety, restart resilience, and release
quality while keeping the rope gameplay server-authoritative and modpack
friendly.

## Highlights

- Added opt-in persisted rope state with world-local JSON storage.
- Restored persisted ropes when required endpoint players come online.
- Added explicit disconnect cleanup through Fabric connection events.
- Added self-escape progress multipliers for guarded and taut situations.
- Added action feedback sounds, configurable separately from particles.
- Added holder damage drop filters by damage type id.
- Added richer `/mcelmarope config` output for the new safety settings.
- Added release jar verification to the Gradle build.
- Added a dedicated server and gameplay testing protocol.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer

## Notes

- `persistRopes` remains disabled by default.
- Automatic cleanup from disconnect, death, spectator mode, dimension mismatch,
  or admin clear still does not refund leads.
- Gameplay decisions remain server-side; the optional client renderer only
  displays rope snapshots.
