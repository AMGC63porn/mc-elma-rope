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
  damage.
- Render active ropes on clients that also install the mod, with smoothed
  endpoint interpolation, configurable sag, and a thicker layered rope look.
- Protect selected players and optionally disable binding near spawn.
- Log rope lifecycle events for moderation when enabled.
- Keep all core state server-side and tick only active rope links.

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
The attempt cancels when the holder is nearby, the rope is taut, the tied
player moves too much, or the tied player takes damage.

Automatic cleanup from disconnect, death, spectator mode, dimension mismatch,
or admin clear does not refund leads.

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
- `enableHolderDamageDrop`: `true`
- `holderDamageDropDenominator`: `100`
- `ropePhysicsPreset`: `custom` (`custom`, `soft`, `balanced`, `strict`)
- `commandPermissionLevel`: `2`
- `maxActiveLinks`: `256`
- `maxHeldDurationTicks`: `0` (`0` means disabled)
- `spawnProtectionRadius`: `0.0`
- `protectedPlayerNames`: `[]`
- `protectedPlayerIds`: `[]`
- `logRopeEvents`: `true`
- `persistRopes`: `false`
- `enableActionFeedbackEffects`: `true`
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

## Compatibility

MC-ELMA Rope does not override the vanilla leash system and does not depend on
Create, Create Aeronautics, Sable, NeoForge, or any client optimization mod.

The MVP uses no mixins and no custom rope entity. It is designed to avoid
conflicts with optimization-heavy Fabric modpacks. Server gameplay works
without the optional client renderer.

## Known Limits

- Rope state is not saved across server restarts.
- The visual renderer is intentionally lightweight and may be improved later
  with textured or thicker rope rendering.
- Automated GameTest coverage is not included yet.

## Build

JDK 21 is required. Use the included Gradle wrapper:

```bash
./gradlew build
```

Successful builds copy the remapped mod jar into the workspace release folder:

```text
fabric-mod-dev/release/mc_elma_rope-0.2.0.jar
```

## License

All Rights Reserved. This repository is public/source-visible for transparency
and official distribution preparation, but it is not open-source licensed.
