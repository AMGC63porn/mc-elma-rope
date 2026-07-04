# Changelog

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
