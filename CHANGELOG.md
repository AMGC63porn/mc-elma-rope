# Changelog

## 0.3.1 - Disconnect Abuse Penalty

- Added configurable lead refund to the controller when a tied target
  disconnects from a lead-created rope.
- Added configurable reconnect penalties for tied targets who disconnect.
- Default reconnect penalty is 2 minutes of Mining Fatigue I and Slowness I.
- Added world-local persistence for pending disconnect penalties.
- Added `/mcelmarope config` output for disconnect refund and penalty settings.
- Bumped mod version to `0.3.1`.

## 0.3.0 - State, Safety, and Release Quality

- Added opt-in rope state persistence with world-local JSON storage and
  join-time restore for online endpoints.
- Added explicit disconnect cleanup through Fabric connection events.
- Added self-escape progress multipliers so guarded or taut attempts can pause
  or progress very slowly instead of only canceling.
- Added optional action feedback sounds separate from particle feedback.
- Added holder damage drop allow/deny damage type filters.
- Added `/mcelmarope config` output for escape multipliers, damage filters,
  feedback sounds, and persistence.
- Added release jar verification to the Gradle build for mod metadata and key
  classes.
- Added `TESTING_PROTOCOL.md` for dedicated server, gameplay, lifecycle, and
  performance validation.
- Bumped mod version to `0.3.0`.

## 0.2.0 - Visual, UX, and Moderation Polish

- Added layered client rope rendering with smoothed endpoints, configurable sag,
  configurable segment count, and visual width presets.
- Added rope visual payload length and taut state for client-side presentation.
- Added action feedback particles for bind, release, rescue, and escape results.
- Added clearer actionbar messages for canceled and completed rope actions.
- Added optional protected player name/UUID lists.
- Added optional spawn protection radius for binding prevention near spawn.
- Added optional max held duration cleanup.
- Added rope event logging for bind, anchor, release, drop, and cleanup events.
- Added `/mcelmarope clearall`, `/mcelmarope inspect <player>`, and
  `/mcelmarope config`.
- Made command permission level and active link limit configurable.
- Added optional rope physics presets: `custom`, `soft`, `balanced`, `strict`.
- Added config load/write warnings instead of silent fallback.
- Bumped mod version to `0.2.0`.

## 0.1.0 - Initial Beta

- Added player-to-player rope creation with the vanilla lead item.
- Added timed bind, controller release, and third-party rescue actions.
- Added configurable lead consumption and manual release refund behavior.
- Added configurable anchor blocks with block id and block tag support.
- Added admin commands for anchor, bind, clear, status, list, and reload.
- Added tied-player self-escape attempts with cooldown, guard radius, taut rope,
  movement, and damage cancellation rules.
- Added low-chance holder damage rope drop behavior.
- Replaced teleport correction with smooth velocity-based rope physics.
- Added optional client rope visuals synced from the server.
- Added server cleanup for disconnect, death, spectator state, and dimension
  mismatch.
