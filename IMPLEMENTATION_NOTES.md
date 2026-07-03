# Implementation Notes

## Current Scope

This is the first gameplay MVP. Physics remains server-authoritative. The only
client-side feature is an optional rope renderer fed by lightweight server
snapshots. There are no custom items, no custom blocks, and no mixins. Vanilla
leads are used as the player interaction item.

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
  cooldown-limited and cancels on nearby holder, taut rope, movement, or damage.
- Holder damage can drop an actively held player-player rope at low chance.
- Rope physics is velocity-based. It removes outward radial velocity and applies
  a capped inward correction to the tied player instead of teleporting players.
- Player-player rope is one-way for physics: the controller is the fixed end,
  and the tied player cannot pull the controller through rope forces.
- Rope visuals are synced through an optional S2C payload. The server sends
  packets only to clients that advertise the visual payload channel.
- Anchor blocks are configurable by block id or block tag.
- Rope links are removed when a player disconnects, dies, becomes spectator, or
  leaves the rope endpoint world. Automatic removal does not refund leads.

## Build Verification

- Built successfully with Eclipse Adoptium JDK 21.0.11 and Gradle 9.6.1.
- The release jar is copied to `fabric-mod-dev/release/mc_elma_rope-0.1.0.jar`.

## Follow-Up Candidates

- Config UI.
- Saved rope state.
- Thicker or textured rope renderer.
- Command suggestions and richer admin feedback.
- Dedicated MC-ELMA test server validation with the full modpack.
- GameTest coverage for rope lifecycle and config behavior.
