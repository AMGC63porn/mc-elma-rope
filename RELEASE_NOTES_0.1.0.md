# MC-ELMA Rope 0.1.0

Initial beta release for Minecraft 1.21.10 on Fabric.

## Highlights

- Server-authoritative player rope gameplay using the vanilla lead item.
- Smooth one-way pulling physics: the controller can pull the tied player, but
  the tied player cannot pull the controller.
- Timed binding, release, third-party rescue, and difficult self-escape actions.
- Configurable anchor blocks and tags.
- Optional client-side rope visuals.
- Admin commands for testing, moderation, and emergency cleanup.

## Requirements

- Minecraft 1.21.10
- Fabric Loader 0.18.4 or newer
- Fabric API 0.138.4+1.21.10 or newer
- Java 21

## Notes

Install the mod on the server for gameplay and physics. Installing it on the
client is optional and enables rope visuals. Rope state is not saved across
server restarts in this beta.
