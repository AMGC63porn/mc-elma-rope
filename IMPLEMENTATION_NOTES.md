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
- Anchor blocks are configurable by block id or block tag.
- Protected player lists, optional spawn protection, optional max held duration,
  damage type filters, and rope event logging are configurable moderation
  controls.
- `persistRopes` is disabled by default. When enabled, active links are saved to
  `mc_elma_rope_state.json` in the world folder and restored when required
  endpoint players are online again.
- Rope links are removed when a player disconnects, dies, becomes spectator, or
  leaves the rope endpoint world. Automatic removal does not refund leads.

## Build Verification

- Built successfully with Eclipse Adoptium JDK 21.0.11 and Gradle 9.6.1.
- `./gradlew build` now also runs `verifyReleaseJar`, which checks release jar
  metadata and required classes.
- The release jar is copied to `fabric-mod-dev/release/mc_elma_rope-0.3.0.jar`.

## Follow-Up Candidates

- Config UI.
- Textured rope renderer.
- Command suggestions and richer admin feedback.
- Dedicated MC-ELMA test server validation with the full modpack.
- GameTest coverage for rope lifecycle and config behavior.
