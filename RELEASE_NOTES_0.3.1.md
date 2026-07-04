# MC-ELMA Rope 0.3.1

MC-ELMA Rope 0.3.1 adds disconnect-abuse handling for tied targets.

## Highlights

- Tied target disconnects can return one lead to the controller.
- Tied targets who disconnect can receive reconnect penalties.
- Default reconnect penalty is 2 minutes of Mining Fatigue I and Slowness I.
- Pending disconnect penalties can persist in the world folder.
- `/mcelmarope config` now reports disconnect refund and penalty settings.

## Compatibility

- Minecraft: `1.21.10`
- Fabric API: `0.138.4+1.21.10`
- Java: `21`
- Server side: required
- Client side: optional, used for the rope renderer

## Notes

- Disconnect penalties apply only when enabled.
- By default, penalties apply only to lead-created ropes.
- Controller disconnects do not punish the tied target.
- Admin clear, death, spectator cleanup, and dimension mismatch still do not
  refund leads.
