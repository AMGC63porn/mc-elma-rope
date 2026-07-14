# MC-ELMA Rope Project Plan

This file tracks the roadmap against the current implementation line. Public
release numbers may advance with smaller patch versions when bug fixes or
test coverage are added between the larger roadmap milestones.

## Current Implementation Line

- Current local version: `0.4.0-beta.1`
- Target Minecraft: `1.21.10`
- Target loader: Fabric
- Fabric API: `0.138.4+1.21.10`
- Server authority: required
- Client install: optional, enables rope visuals
- Core rule: gameplay decisions stay server-side

## Completed In Current 0.4.0-beta Line

- Vanilla lead based player binding.
- Timed bind, controller release, third-party rescue, and self-escape actions.
- Configurable anchor block ids and block tags.
- One-way smooth velocity rope physics: the tied player cannot pull the
  controller.
- Optional vanilla-like client rope renderer with lightweight visual sync.
- Configurable disconnect refund and reconnect penalties.
- Optional persistence for rope state and pending disconnect penalties.
- Offline anchored rope persistence when an anchored tied player disconnects.
- Moderation controls: protected players, spawn protection, max held duration,
  event logging, holder damage drop filters.
- Admin commands for bind, anchor, clear, clearall, status, inspect, list,
  config, and reload.
- Fabric GameTest smoke coverage for gameplay, lifecycle, persistence, config,
  performance, physics, and moderation invariants.
- Anchor break cleanup: when the block used as an anchor is broken, matching
  anchored ropes are released automatically without lead refund.
- Offline anchor break cleanup: breaking the anchor also removes pending
  offline anchored rope records.

## 0.2.0 Roadmap Status - Visual And UX

Status: implemented in the current branch.

- Vanilla-like rope rendering: done.
- Legacy layered rope rendering as config fallback: done.
- Client-optional renderer: done.
- Smoothed endpoint interpolation and sag: done.
- Actionbar messages and feedback effects: done.
- Readable status/list/config command output: implemented at smoke-test level.

## 0.3.0 Roadmap Status - Safety, Admin, State

Status: implemented and still being hardened through 0.3.x patches.

- Protected players and spawn protection: done.
- Max held duration: done.
- Event logging: done.
- Inspect, clearall, config commands: done.
- Optional persistence: done.
- Disconnect cleanup/refund/penalty: done.
- Anchor block break cleanup: done in `0.3.13`.
- Anchored disconnect persistence: done in `0.4.0-beta.1`.

## 0.4.0 Roadmap Status - Physics And Roleplay Balance

Status: partially implemented, now in `0.4.0-beta.1`.

- No teleport rope correction: done.
- One-way player-player rope force: done.
- Pull speed, damping, and physics presets: done.
- Anchor hanging, reconnect persistence, and swing behavior: functional; still
  needs multiplayer feel testing on the MC-ELMA test server.
- Self-escape balance: functional; still needs live tuning.
- Holder damage drop: done; live denominator and damage filters need tuning.

## 0.5.0 Roadmap Status - Test And Release Quality

Status: partially implemented.

- Gradle build verification: done.
- Headless GameTest runner: done.
- Smoke tests for core systems: done.
- Performance smoke coverage for 0/50/100/256 links: done.
- Remaining work: real MC-ELMA modpack server validation, full multiplayer
  gameplay test pass, release checklist execution, and Modrinth draft update.

## Next Recommended Work

- Run a real multiplayer test pass for `0.4.0-beta.1` on the MC-ELMA test
  server.
- Verify anchor break cleanup in-game with fence, wall, chain, iron bars,
  lightning rod, end rod, and bell anchors.
- Verify anchored disconnect/reconnect with client mod installed and absent.
- Verify vanilla-like rope visual readability in normal gameplay lighting.
- Tune roleplay balance values after real player testing:
  self-escape chance, guard radius, taut slowdown, holder damage drop chance,
  disconnect penalty severity, and max held duration if enabled.
- Decide when `0.4.0-beta.1` is stable enough to publish as a wider beta.
