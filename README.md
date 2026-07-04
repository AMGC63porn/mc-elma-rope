# MC-ELMA Rope

MC-ELMA Rope is a Fabric mod for Minecraft 1.21.10 that turns the vanilla
lead into a server-authoritative player rope system. It is built for
multiplayer survival servers that want controllable tying, rescue, escape,
anchor, and smooth pulling mechanics without replacing vanilla leash behavior.

The mod can run on a dedicated server as the authority for gameplay and
physics. Installing it on the client is optional and enables the lightweight
rope visual renderer.

## Features

- Bind another player with a vanilla lead after a timed 3 second action.
- Pull the tied player with smooth velocity-based rope physics instead of
  teleport correction.
- Keep rope physics one-way: the tied player cannot pull the controller.
- Tie a carried player to configured anchor blocks such as fences, walls,
  chains, iron bars, lightning rods, end rods, and bells.
- Release ropes manually with timed actions and return a lead when appropriate.
- Allow configurable third-party rescue actions for unbound players.
- Give tied players a difficult, low-chance self-escape attempt.
- Let a rope slip free at a configurable low chance when the holder takes
  damage, with optional damage type allow/deny filters.
- Render active ropes on clients that also install the mod, with smoothed
  endpoint interpolation, configurable sag, and a thicker layered rope look.
- Keep optional rope visual sync cheap by avoiding active-link processing while
  no ropes exist and reusing per-world snapshots during sync.
- Protect selected players and optionally disable binding near spawn.
- Optionally persist rope state across restart and restore it when endpoint
  players rejoin.
- Return a lead to the controller when a tied target disconnects, and apply
  configurable reconnect penalties to discourage disconnect abuse.
- Log rope lifecycle events for moderation when enabled.
- Keep all core state server-side and tick only active rope links.
- Ship Fabric GameTest smoke coverage for config reload, manager, disconnect
  penalty, lifecycle, performance, persistence, physics, and timed action
  invariants.

## Requirements

- Minecraft 1.21.10
- Fabric Loader 0.18.4 or newer
- Fabric API 0.138.4+1.21.10 or newer
- Java 21

## Installation

Install the jar on the server to enable gameplay and physics. Install the same
jar on clients when rope visuals are desired.

The first server launch creates:

```text
config/mc_elma_rope.json
```

## Gameplay

Use a lead on another player and keep the interaction conditions valid until
the action completes. Survival and adventure players consume one lead when the
rope is created. Creative players can create ropes without item consumption.

The controller can release the rope with an empty main hand after the configured
release duration. If the rope was created by consuming a lead, manual release
refunds one lead to the releasing player.

When enabled, another unbound player can rescue the tied player with a slower
timed release. The tied player cannot release other players' ropes and cannot
bind another player while tied.

A tied player can attempt a self-escape with an empty main hand. By default the
attempt takes 45 seconds, has a 60 second cooldown, and succeeds at 1 in 300.
The attempt pauses while the holder is nearby, cancels when the rope is taut by
default, and cancels when the tied player moves too much or takes damage. Server
owners can change guarded or taut escape attempts from "paused/canceled" to
"very slow" through config.

Automatic cleanup from death, spectator mode, dimension mismatch, or admin clear
does not refund leads. Disconnect cleanup can optionally return one lead to the
controller when the tied target disconnects from a lead-created rope.

By default, a tied target who disconnects is marked for a reconnect penalty.
When they return, they receive 2 minutes of Mining Fatigue I and Slowness I.
Server owners can disable this, change effect levels, change duration, or
disable persistence of pending penalties.

## Commands

Admin commands are intended for testing, moderation, and emergency cleanup.

```text
/mcelmarope anchor <player> <x> <y> <z> <length>
/mcelmarope bind <playerA> <playerB> <length>
/mcelmarope clear <player|all>
/mcelmarope clearall
/mcelmarope status <player>
/mcelmarope inspect <player>
/mcelmarope list
/mcelmarope config
/mcelmarope reload
```

## Configuration

Important defaults:

- `maxRopeLength`: `48.0`
- `defaultPlayerRopeLength`: `12.0`
- `anchorRopeLength`: `12.0`
- `bindDurationTicks`: `60`
- `controllerReleaseDurationTicks`: `60`
- `thirdPartyReleaseDurationTicks`: `240`
- `enableThirdPartyRelease`: `true`
- `enableSelfEscape`: `true`
- `selfEscapeDurationTicks`: `900`
- `selfEscapeCooldownTicks`: `1200`
- `selfEscapeSuccessDenominator`: `300`
- `selfEscapeGuardProgressMultiplier`: `0.0`
- `selfEscapeTautProgressMultiplier`: `0.15`
- `enableHolderDamageDrop`: `true`
- `holderDamageDropDenominator`: `100`
- `holderDamageDropAllowedDamageTypeIds`: `[]`
- `holderDamageDropDeniedDamageTypeIds`: `[]`
- `ropePhysicsPreset`: `custom` (`custom`, `soft`, `balanced`, `strict`)
- `commandPermissionLevel`: `2`
- `maxActiveLinks`: `256`
- `maxHeldDurationTicks`: `0` (`0` means disabled)
- `spawnProtectionRadius`: `0.0`
- `protectedPlayerNames`: `[]`
- `protectedPlayerIds`: `[]`
- `logRopeEvents`: `true`
- `persistRopes`: `false`
- `refundLeadToControllerOnTargetDisconnect`: `true`
- `enableDisconnectPenalty`: `true`
- `persistDisconnectPenalties`: `true`
- `disconnectPenaltyOnlyLeadCreatedRopes`: `true`
- `disconnectPenaltyDurationTicks`: `2400`
- `disconnectPenaltyMiningFatigueLevel`: `1`
- `disconnectPenaltySlownessLevel`: `1`
- `disconnectPenaltyShowParticles`: `true`
- `disconnectPenaltyShowIcon`: `true`
- `enableActionFeedbackEffects`: `true`
- `enableActionFeedbackSounds`: `true`
- `ropeVisualEnabled`: `true`
- `ropeVisualSegments`: `20`
- `ropeVisualSag`: `0.045`
- `ropeVisualWidthPreset`: `balanced`

Anchor entries support block ids and block tags:

```json
[
  "#minecraft:fences",
  "#minecraft:fence_gates",
  "#minecraft:walls",
  "minecraft:chain",
  "minecraft:iron_bars",
  "minecraft:lightning_rod",
  "minecraft:end_rod",
  "minecraft:bell"
]
```

Use `/mcelmarope reload` after editing the config.

When `persistRopes` is enabled, rope state is saved into the world folder as
`mc_elma_rope_state.json`. Player-player ropes restore only after the required
players are online again. Normal disconnect cleanup during gameplay still
removes active ropes and does not refund leads.

When `persistDisconnectPenalties` is enabled, pending reconnect penalties are
saved into the world folder as `mc_elma_rope_disconnect_penalties.json`.

## Compatibility

MC-ELMA Rope does not override the vanilla leash system and does not depend on
Create, Create Aeronautics, Sable, NeoForge, or any client optimization mod.

The MVP uses no mixins and no custom rope entity. It is designed to avoid
conflicts with optimization-heavy Fabric modpacks. Server gameplay works
without the optional client renderer.

## Known Limits

- Rope state persistence is opt-in and disabled by default.
- The visual renderer is intentionally lightweight and may be improved later
  with textured or thicker rope rendering.
- GameTest coverage is currently smoke-level; full multiplayer gameplay
  validation still belongs in the MC-ELMA test server protocol.

See [TESTING_PROTOCOL.md](TESTING_PROTOCOL.md) for the dedicated server and
gameplay validation checklist.

## Build

JDK 21 is required. Use the included Gradle wrapper:

```bash
./gradlew build
```

Successful builds verify the release jar metadata and copy the remapped mod jar
into the workspace release folder:

```text
fabric-mod-dev/release/mc_elma_rope-0.3.9.jar
```

## License

All Rights Reserved. This repository is public/source-visible for transparency
and official distribution preparation, but it is not open-source licensed.
