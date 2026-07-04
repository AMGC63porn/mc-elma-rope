# MC-ELMA Rope 0.2.0

Visual, UX, and moderation polish release for Minecraft 1.21.10 on Fabric.

## Highlights

- Thicker layered rope renderer with endpoint smoothing and configurable sag.
- Rope visuals now account for rope length and taut state.
- Cleaner actionbar feedback for tying, releasing, rescuing, and escaping.
- Small optional particle feedback for successful and failed rope actions.
- Protected player lists and optional spawn protection radius.
- Optional max held duration cleanup for moderation-heavy servers.
- Rope lifecycle logging for server operators.
- New admin tools: `/mcelmarope clearall`, `/mcelmarope inspect <player>`, and
  `/mcelmarope config`.

## Requirements

- Minecraft 1.21.10
- Fabric Loader 0.18.4 or newer
- Fabric API 0.138.4+1.21.10 or newer
- Java 21

## Notes

Install the mod on the server for gameplay and physics. Installing it on the
client remains optional and enables improved rope visuals. Gameplay decisions
remain server-authoritative.
