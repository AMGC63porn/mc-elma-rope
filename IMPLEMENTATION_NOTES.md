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
  only; gameplay remains server-authoritative. The default renderer is a
  vanilla-like procedural braided rope; the legacy layered-line style is still
  available through config.
- Rope visual sync sends one empty state after links clear, skips active-link
  processing while no ropes exist, and caches visual link snapshots per world
  for each sync tick.
- Anchor blocks are configurable by block id or block tag.
- Anchored ropes listen for server-side anchor block break events. If the
  stored anchor block is broken, matching anchored ropes are removed without
  lead refund because this is automatic cleanup, not manual release.
- Anchored target disconnects can persist as offline anchored rope records in
  `mc_elma_rope_anchored_offline.json`. On reconnect, the rope restores only if
  the player is in the anchor world and the anchor block is still valid.
- During an orderly server shutdown, active anchored ropes are captured into
  the same offline record format. This preserves anchored ropes through restart
  without enabling general player-player rope persistence.
- Protected player lists, optional spawn protection, optional max held duration,
  damage type filters, and rope event logging are configurable moderation
  controls.
- `persistRopes` is disabled by default. When enabled, active links are saved to
  `mc_elma_rope_state.json` in the world folder and restored when required
  endpoint players are online again.
- If a held player-player target disconnects from a lead-created rope, the
  controller can receive one lead back and the target can receive configurable
  reconnect penalties. Anchored target disconnects do not refund leads because
  the rope is still considered active through offline anchored state. Pending
  penalties can persist in
  `mc_elma_rope_disconnect_penalties.json`.
- Rope links are removed when a player disconnects, dies, becomes spectator, or
  leaves the rope endpoint world. Automatic removal does not refund leads.
- Fabric GameTest smoke suites are packaged for config defaults, default anchor
  support, manager cleanup, length clamping, persisted restore duplicate
  protection, disconnect penalty behavior, lifecycle cleanup, and one-way rope
  physics behavior, timed gameplay action behavior, and config reload
  normalization behavior, world-local rope persistence behavior, active-link
  performance scale behavior, disconnect refund/penalty boundaries, and
  anchored offline reconnect behavior.

## Build Verification

- Built successfully with Eclipse Adoptium JDK 21.0.11 and Gradle 9.6.1.
- `./gradlew build` now also runs `verifyGameplayInvariants` and
  `verifyReleaseJar`, which check source-level design invariants, release jar
  metadata, required classes, and the GameTest entrypoint.
- `scripts/run-headless-gametests.sh` runs the Fabric GameTest suite in a
  headless server and writes an XML report under `build/gametest-results/`.
- The release jar is copied to
  `fabric-mod-dev/release/mc_elma_rope-0.4.0-beta.1.jar`.
- A local Fabric dedicated server dev-runtime smoke test has booted
  `mc_elma_rope 0.3.10`; see `SERVER_SMOKE_REPORT.md`.

## Follow-Up Candidates

- Config UI.
- Textured rope renderer.
- Command suggestions and richer admin feedback.
- Full dedicated server smoke tests with the MC-ELMA modpack.
