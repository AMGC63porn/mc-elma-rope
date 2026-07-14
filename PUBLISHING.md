# Publishing Checklist

This file is the handoff checklist for GitHub and Modrinth publication.

## GitHub

- Repository name: `mc-elma-rope`
- Visibility: public
- Default branch: `main`
- Description: `Server-authoritative Fabric rope and leash gameplay for Minecraft 1.21.10.`
- Suggested topics: `minecraft`, `fabric`, `minecraft-mod`, `rope`, `leash`, `mc-elma`
- License: All Rights Reserved
- Release tag: `v0.4.0-beta.1`
- Release title: `MC-ELMA Rope 0.4.0-beta.1`
- Release asset: `mc_elma_rope-0.4.0-beta.1.jar`
- Repository URL: `https://github.com/AMGC63porn/mc-elma-rope`
- Release URL: `https://github.com/AMGC63porn/mc-elma-rope/releases/tag/v0.4.0-beta.1`

Do not commit generated build folders or release jars to the source repository.
Attach the jar to the GitHub Release instead.

## Modrinth

- Project type: mod
- Title: `MC-ELMA Rope`
- Slug: `mc-elma-rope`
- Loader: Fabric
- Client side: optional
- Server side: required
- License: custom / All Rights Reserved, linked to the repository license when
  possible
- Source URL: `https://github.com/AMGC63porn/mc-elma-rope`
- Issues URL: `https://github.com/AMGC63porn/mc-elma-rope/issues`
- Icon: `publishing/modrinth-icon.svg`

## Modrinth Version 0.4.0-beta.1

- Version number: `0.4.0-beta.1`
- Version type: beta
- Game version: `1.21.10`
- Loader: Fabric
- Dependency: Fabric API `0.138.4+1.21.10` required
- File: `mc_elma_rope-0.4.0-beta.1.jar`
- SHA-256:
  `f5d86746e86aebe378b8fda6b8162a2a155ad93959538d38dbfcdd9fa26c859e`

Publish to draft/unlisted first, test with the MC-ELMA server, then list the
project publicly after gameplay validation.

## Pre-Publish Validation

- Build the project with Java 21.
- Confirm the remapped jar exists in `fabric-mod-dev/release/`.
- Confirm `fabric.mod.json` metadata in the jar.
- Boot a dedicated Fabric server with Fabric API.
- Follow `TESTING_PROTOCOL.md`.
- Smoke test binding, release, third-party rescue, self-escape, anchor tying,
  smooth pulling physics, and optional client visuals.
