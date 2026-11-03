# Ironclad Empire

A real-time strategy mod for Minecraft. Lead a village from above: gather resources, build from blueprints, hire
professions, raise an army and grow your settlement, all with the mouse, like a classic RTS.

Ironclad Empire is a continued fork of **[MineFortress](https://github.com/remmintan/minefortress)** by remmintan,
which has not been updated since August 2025. All credit for the original mod goes to its author.
Licence: MIT, same as the original (see [LICENSE](LICENSE)).

> **Status: alpha.** Runs on **Minecraft 1.21.1 (Fabric)**. Expect bugs; please report them in
> [Issues](https://github.com/Lammy93/ironclad-empire/issues).

## Install

1. Create a **Fabric** instance for **Minecraft 1.21.1** (Modrinth App, Prism Launcher, or the Fabric installer).
2. Put these in its `mods` folder:
   - Ironclad Empire (`ironclad-empire-*.jar`)
   - [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.1
   - [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
3. Start the game and create a new world with the **Fortress** game mode.

Servers need the same mods. Players without Ironclad Empire are turned away with a message telling them to install it.

## What's different from MineFortress

- Ported from Minecraft 1.20.2 to **1.21.1**.
- Fixes: dedicated servers start again, storage no longer loads far-away chunks or forgets out-of-range chests, no
  start-up crash without the "Keybind Fix" mod (shared keys are handled built in).
- **No data sent anywhere:** the original sent each player's name to its website for a supporter check. That is
  removed, and every pawn skin is available to everyone.
- Planned: the original mod's open bug reports, protection between fortresses on shared servers, a versus mode,
  and more (see Issues).

## Building from source

Everything builds in Docker (JDK 21); nothing needs installing on the host.

```bash
git clone --recurse-submodules https://github.com/Lammy93/ironclad-empire.git
cd ironclad-empire
scripts/build-deps.sh          # once: builds our Automatone fork (pathfinding) into deps/m2
scripts/gradle.sh build        # the mod jar ends up in build/libs/
```

Checks:

| Command | What it does |
|---|---|
| `scripts/gametest.sh` | Headless Fabric game tests on a dedicated server |
| `scripts/client-check.sh audit` | Starts the client headless and applies every mixin up front |
| `scripts/client-check.sh boot` | Boots the client headless to the title screen |
| `scripts/prodcheck.sh` | Checks the shipped (remapped) jar for unimplemented methods |
| `scripts/vanilla-join-check.sh` | Joins with a vanilla client and expects a clean "install the mod" message |
| `scripts/predrop-check.sh` | All of the above that gate a test build |

Porting notes per Minecraft version: [docs/porting-notes.md](docs/porting-notes.md).
