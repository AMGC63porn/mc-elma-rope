# MC-ELMA Rope Testing Protocol

Use this checklist before tagging or publishing a release.

## Automated Local Checks

Run with JDK 21:

```bash
./gradlew build
```

The build runs `verifyGameplayInvariants` and `verifyReleaseJar`, which check:

- no mixin configuration is introduced
- teleport rope correction does not return
- the mod remains server-safe/client-optional in metadata
- critical gameplay defaults stay pinned
- visual sync keeps client packets optional and avoids active-link work while
  idle
- the release jar exists in `fabric-mod-dev/release/`
- `fabric.mod.json` exists in the jar
- mod id and version match Gradle metadata
- Minecraft and Fabric API dependencies match the target versions
- required server and optional client classes are present
- the Fabric GameTest entrypoint and smoke test class are present

## Dedicated Server Boot

- Start a Fabric dedicated server for Minecraft `1.21.10`.
- Install Fabric API `0.138.4+1.21.10`.
- Install `mc_elma_rope-0.3.8.jar` on the server.
- Confirm startup creates or reads `config/mc_elma_rope.json`.
- Confirm no client-only classloading crash occurs.

## Fabric GameTest Smoke Coverage

The release jar declares `com.mcelma.rope.test.McElmaRopeGameTests` under the
`fabric-gametest` entrypoint. These smoke tests cover:

- default config values
- default fence and chain anchor support
- manager self-link rejection
- anchor add, length clamping, and cleanup
- persisted link restore duplicate protection
- tied target disconnect penalty queue and reconnect effect application
- controller disconnect cleanup without punishing the tied target
- controller release and anchored release permission behavior
- dead and spectator endpoint cleanup
- one-way taut rope physics and loose rope no-op behavior
- timed bind with lead consumption
- timed controller release and slower third-party rescue
- tied-player rescue denial
- timed anchor tying
- default taut self-escape denial
- config value sanitization and malformed config fallback
- custom anchor id/tag reload
- protected player name reload
- persisted player-player rope save/load restore
- persisted anchored rope save/load restore
- pending restore behavior for missing player endpoints

Run these tests with Fabric's GameTest server tooling when the MC-ELMA test
environment is available. They are smoke tests, not a replacement for the
multiplayer gameplay checklist below.

## Optional Client Renderer

- Join once without the client mod and confirm gameplay still works.
- Join once with the client mod and confirm visible rope rendering works.
- Confirm visual packets do not block gameplay when the client mod is absent.

## Gameplay Smoke Tests

- Survival player binds another player with a lead after `3s`.
- Survival bind consumes one lead.
- Controller releases after `3s` with empty main hand and receives one lead.
- Third-party rescue works after `12s` when enabled.
- Third-party rescue fails when `enableThirdPartyRelease=false`.
- Tied players cannot bind another player.
- Tied players cannot release another player's rope.
- Anchor binding works on fences, fence gates, walls, chains, iron bars,
  lightning rods, end rods, and bells.
- Anchor release returns one lead on manual release when eligible.

## Self Escape

- Tied player starts escape with empty main hand.
- Escape progress pauses while the holder is inside `selfEscapeGuardRadius`
  when `selfEscapeGuardProgressMultiplier=0.0`.
- Escape cancels while taut when `selfEscapeCancelWhenTaut=true`.
- Escape progresses slowly while taut when `selfEscapeCancelWhenTaut=false`.
- Escape cancels when the tied player moves too far.
- Escape cancels when the tied player takes damage.
- Escape completion respects `selfEscapeSuccessDenominator`.
- Cooldown prevents immediate repeated attempts.

## Holder Damage Drop

- Holder damage drop respects `holderDamageDropDenominator`.
- Denied damage type ids never trigger drop.
- Non-empty allowed damage type ids restrict the drop roll to that list.
- Anchor ropes do not drop from holder damage.

## Lifecycle

- Disconnect clears active ropes.
- Tied target disconnect returns one lead to the controller when
  `refundLeadToControllerOnTargetDisconnect=true`.
- Controller disconnect does not punish the tied target.
- Tied target reconnect receives Mining Fatigue I and Slowness I for 2 minutes
  with default penalty settings.
- `enableDisconnectPenalty=false` prevents reconnect penalty application.
- `persistDisconnectPenalties=true` preserves pending penalties through server
  restart.
- Death clears active ropes and does not refund leads.
- Spectator mode clears active ropes and does not refund leads.
- Dimension or portal mismatch clears active ropes and does not refund leads.
- `maxHeldDurationTicks` clears old ropes when enabled.
- `persistRopes=false` keeps restart behavior non-persistent.
- `persistRopes=true` saves world-local state and restores when endpoint players
  rejoin.

## Performance

- Confirm idle server cost with `0` active ropes.
- Confirm only active links tick with `50`, `100`, and `256` active ropes.
- Confirm `/mcelmarope list` remains readable and bounded.
