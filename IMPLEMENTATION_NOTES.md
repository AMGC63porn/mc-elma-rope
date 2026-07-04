# Implementation Notes

## Current Scope

This is a server-authoritative gameplay and polish build. The only client-side
feature is an optional rope renderer fed by lightweight server snapshots. There
are no custom items, no custom blocks, and no mixins. Vanilla leads are used as
the player interaction item.

## Compatibility Notes

- Rope state is stored in an active in-memory list.
- The manager ticks only active rope links.
- A player can be in only one MVP rope link at a time.
- A linked player cannot start a new rope link.
- Rope interactions are timed server actions with gaze, distance, hand, and
  cooldown validation.
- The default item-driven rope length is configurable and capped at 48 blocks.
- Lead-created ropes consume one lead outside creative and refund one lead on
  manual release to the releasing player.
- Third-party release is configurable and requires the rescuer to be unbound.
- Self-escape is a long, low-chance active action for the tied player. It is
  cooldown-limited, pauses or slows near the holder, can cancel or slow while
  taut, and cancels on movement or damage.
- Holder damage can drop an actively held player-player rope at low chance and
  can be filtered by damage type id.
- Rope physics is velocity-based. It removes outward radial velocity and applies
  a capped inward correction to the tied player instead of teleporting players.
- Player-player rope is one-way for physics: the controller is the fixed end,
  and the tied player cannot pull the controller through rope forces.
- Rope visuals are synced through an optional S2C payload. The server sends
  packets only to clients that advertise the visual payload channel.
- Rope visuals include length and taut state for client-side sag/interpolation
  only; gameplay remains server-authoritative.
- Rope visual sync sends one empty state after links clear, skips active-link
  processing while no ropes exist, and caches visual link snapshots per world
  for each sync tick.
- Anchor blocks are configurable by block id or block tag.
- Protected player lists, optional spawn protection, optional max held duration,
  damage type filters, and rope event logging are configurable moderation
  controls.
- `persistRopes` is disabled by default. When enabled, active links are saved to
  `mc_elma_rope_state.json` in the world folder and restored when required
  endpoint players are online again.
- If a tied target disconnects from a lead-created rope, the controller can
  receive one lead back and the target can receive configurable reconnect
  penalties. Pending penalties can persist in
  `mc_elma_rope_disconnect_penalties.json`.
- Rope links are removed when a player disconnects, dies, becomes spectator, or
  leaves the rope endpoint world. Automatic removal does not refund leads.
- Fabric GameTest smoke suites are packaged for config defaults, default anchor
  support, manager cleanup, length clamping, persisted restore duplicate
  protection, disconnect penalty behavior, lifecycle cleanup, and one-way rope
  physics behavior, timed gameplay action behavior, and config reload
  normalization behavior, world-local rope persistence behavior, active-link
  performance scale behavior, and disconnect refund/penalty boundaries.

## Build Verification

- Built successfully with Eclipse Adoptium JDK 21.0.11 and Gradle 9.6.1.
- `./gradlew build` now also runs `verifyGameplayInvariants` and
  `verifyReleaseJar`, which check source-level design invariants, release jar
  metadata, required classes, and the GameTest entrypoint.
- The release jar is copied to `fabric-mod-dev/release/mc_elma_rope-0.3.10.jar`.
- A local Fabric dedicated server dev-runtime smoke test has booted
  `mc_elma_rope 0.3.10`; see `SERVER_SMOKE_REPORT.md`.

## Follow-Up Candidates

- Config UI.
- Textured rope renderer.
- Command suggestions and richer admin feedback.
- Full dedicated server smoke tests with the MC-ELMA modpack.
