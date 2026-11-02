# MineFortress Continued — Phase 1: Port to Fabric 1.21.1 and fix open bugs

Date: 2026-11-02
Status: approved by Zac 2026-11-02

## 1. Goal

Bring the MineFortress RTS mod (remmintan/minefortress, MIT, last code August 2025,
Minecraft 1.20.2) up to **Fabric / Minecraft 1.21.1** as a continued fork, and fix the
13 open bug reports on the original repo. Phase 1 changes no gameplay on purpose,
apart from the bug fixes.

### Where Phase 1 sits in the whole project

The wider aim is one current mod that combines four downloaded files. Each phase gets
its own design, plan and build cycle:

| Phase | Content | Source / licence |
|---|---|---|
| **1 (this spec)** | Port + bug fixes | MineFortress, MIT |
| 2 | Survival ↔ fortress hotkey swap, 9 extra recipes (covers upstream #62) | MFAdditions by JGBau, AFL-3.0 |
| 3 | Combat stances/formations/squads, auto work zones (trees, mines, fishing), shared-storage crafting + missing-materials list, factions/safe zones, mounts/boats, Structurize import | **Ideas only** from "MineFortress Reforged" (deletethe0, All Rights Reserved, NeoForge 1.21.1). Clean reimplementation; no Reforged code or assets are copied. |
| 4 | Our own retextured pawn skins as an optional built-in skin set (one per profession, soldier colour variants) | Original artwork made for this project (decided 2026-11-02). Elithris's pack is not used or copied |

The project must stay **publishable**: only MIT/AFL code, our own code, and assets
we have permission for.

## 2. Decisions already made

- Loader/version: **Fabric, Minecraft 1.21.1**, Java 21. (26.x is a later, separate port.)
- Approach: **fork and step-port**, carrying our own port of the Automatone pathfinding
  fork, so pawn movement stays the same. Pathfinding replacement is postponed to Phase 3.
- Name: **MineFortress Continued**. Mod id stays **`minefortress`** so existing
  worlds load. Display name and jar name change. Version line continues at **1.18.0**.
- Repos: public GitHub forks under **Lammy93**: `minefortress` (from upstream
  `master`, which includes 17 unreleased commits after 1.17.5-beta) and `automatone`
  (from `minefortress-mod/automatone`). Local working copy:
  `/mnt/user/claude-workspace/minefortress-continued/`.
- Commit identity: `LammyK <121271206+Lammy93@users.noreply.github.com>`.
- Credits in `fabric.mod.json` and README: remmintan (original author). JGBau (MFAdditions
  ideas) gets added in Phase 2.
- No publishing to Modrinth/CurseForge in Phase 1.

## 3. Starting point (facts checked 2026-11-02)

- Upstream code: ~35k lines across 7 Gradle source sets (`core`, `networking`, `gui`,
  `building`, `selections`, `panama`, `main`), Java + Kotlin. Loom 1.4, Kotlin 1.9.21,
  Yarn 1.20.2+build.4, Fabric API 0.91.6, Java 17.
- Bundled dependencies (jar-in-jar):
  - **Fabric-ASM v2.3** (Chocohead, no updates since Dec 2023): used only by
    `GameModeEarlyRiser` to inject `GameMode.FORTRESS` and a matching
    game-mode-switcher entry. `FORTRESS` is referenced in 132 places.
  - **Automatone 1.0.9-minefortress** (Baritone-for-entities fork, ~33k lines, 1.20.2
    only; no 1.21 build exists anywhere). MineFortress uses a thin slice of it from
    10 files: `IBaritone`, `BaritoneAPI`, `GoalNear`, `PathEvent`, `BetterBlockPos`,
    `IMinefortressEntity`, `IFortressAwareBlockEntity`.
  - **Cardinal Components 5.2.9-minefortress** (custom fork). The official CCA
    **6.1.3** supports 1.21.1.
- Old-style networking in 65 files (`PacketByteBuf`). Item/NBT handling in 48 files.
- `master` holds 17 unreleased commits (2025-07-05 → 2025-08-10): a resource-system
  rewrite that stores items in real chests through the Fabric Transfer API, a
  per-task physical storage, removal of the legacy crafting/inventory screens, and new
  health/progress bars. Whether it is complete is unknown.

## 4. Port procedure

Every step ends with: the project compiles, a dev client and a dedicated server start,
and the **smoke game-tests** pass (section 6). Steps are never merged together.

| Step | Target | Work |
|---|---|---|
| 0 | 1.20.2 (no game change) | Gradle wrapper and Loom to versions that run on JDK 21. Current Kotlin + fabric-language-kotlin. Build Automatone from our fork as a source dependency (composite build or local maven) instead of JitPack. Add the game-test source set. **Play-test the unreleased storage rewrite.** If it is unfinished, finishing it is the first task, before Step 1. |
| 1 | 1.20.4 | API and mapping updates (mod still targets Java 17). **Check whether Fabric-ASM still works** on Fabric Loader 0.16.x (load a world, switch to fortress mode, use the F3+F4 switcher). |
| 2 | 1.20.6 | Item NBT → **data components**. Networking → `CustomPayload` + `PacketCodec` with the Fabric payload registry. Converted one packet / one item feature at a time. The mod targets Java 21 from here (Minecraft 1.20.5+ requires it). |
| 3 | 1.21.1 | Registry/data-pack path changes, enchantments as data, rendering and screen API updates, `Identifier` constructor changes. CCA → official 6.1.3 (drop the custom fork unless it carries needed patches; diff it first). |

Automatone is ported in lock-step with every step.

### Fortress game-mode fallback

If Fabric-ASM fails at Step 1 or later, replace the injected enum with a
**per-player fortress-mode flag**: the player stays in survival or creative
underneath, the flag is stored on the player through a CCA component, and the
existing `FortressGamemodeUtils` becomes the single place the rest of the code
asks. The `/gamemode fortress` behaviour becomes a `/fortress` command (enter/exit) and
a keybind. Existing worlds that saved a player in game mode id 4 get mapped to
survival + flag on load. This fallback is also the leading hypothesis for #39/#63.

### World compatibility check (end of Step 3)

A world created and played on **1.17.5-beta / 1.20.2** (fortress with pawns of at
least three professions, built buildings, filled storage, one custom blueprint) is
opened on the 1.21.1 build. Pass = all of these keep working without errors in the
log. The test world is kept in the repo under `testworlds/` (small, zipped).

## 5. Bug fixes

Each fix starts with a game-test (or, where the bug lives in the client, a
documented manual repro step) that fails, then the fix, then the test passes.
Fix order: crash/data-loss first.

| Upstream # | Bug | Planned fix |
|---|---|---|
| 26 | Server crash (`ServerTaskManager.tick`, null player) when the owner leaves mid-task | Tasks look up the owner each tick and pause while the owner is offline; no player dereference without a null check. |
| 63, 39 | Crash hovering MineFortress buttons with Create, Botania, Fabric Seasons, Farmer's Delight addons (null `BlockPos` in the hover raycast) | Guard the null raycast result; reproduce in a dev instance with Create (Fabric) installed. Re-test after the game-mode decision. |
| 49 | "Explore your village!" wipes the player's own inventory | The survival inventory is saved to the player's fortress data on entry and restored on exit. Never cleared. |
| 30 | Blocks deleted when stacking/dropping after a crafting-tab fill | Covered by the real-chest storage. The test fills the hotbar from storage, then stacks, shift-clicks and Q-drops: the item count must be conserved. |
| 36 | Crafting packet error / item duplication (`ClickSlotC2SPacket`) | Crafting goes through server-side storage transactions only. The client sends a "craft N of recipe R" request; no client-side slot clicks. Test: crafting with ingredients for 2 crafts produces exactly 2 results and uses exactly 2× ingredients. |
| 51 | Pawns lose their profession after editing a blueprint | Profession counts are rebuilt from persistent pawn data after the blueprint editor closes and on world load. |
| 50 | Pawns become jobless (linked to unloading) | The same rebuild, plus profession assignment keyed by pawn UUID, so unloaded pawns keep their job and a new assignment never strips an unloaded pawn. |
| 48 | Pawns can destroy the campfire → softlock | The fortress centre is excluded from all pawn dig/clear tasks. A "Rebuild campfire" action appears if the centre is missing. |
| 27 | Blueprint editor errors with Dynmap (null world in Dynmap) | Create the editor dimension through the normal registered-dimension path so other mods get their world-load events. Test with Dynmap installed. |
| 33 | Pawns stuck inside fence + pressure-plate tables | After placing a block, a pawn standing inside a non-full block moves to the nearest free spot. Blueprint placement order puts such "furniture" blocks last. |
| 29 | Army does not fight back when idle | Idle warriors, archers and knights retaliate against attackers and nearby threats. Full stances come in Phase 3. |
| 45 | "This world is no longer supported" on very old worlds | Deliberate upstream block of pre-rework worlds: keep it, reword it to explain what to do. |

Not in Phase 1: #62 (Phase 2), #9 translation prep, #20/#31/#34/#47 (meta/roadmap;
#34/#47 are answered by this port).

## 6. Testing

- **Fabric game-tests**, run headless in the Docker build on every build:
  - Smoke set: create a fortress, spawn pawns, place and complete a small blueprint,
    chop trees, kill one hostile, save and reload the world, and check that the state survived.
  - One regression test per bug in section 5 (except client-only #63/#39, which get a
    scripted manual check).
- **Build environment:** a throwaway Docker container (`eclipse-temurin:21-jdk`) on
  zraid with the Gradle cache in a named volume; nothing is installed on the host.
- **Play-testing:** after each port step and at the end, the jar and a ready Fabric
  1.21.1 instance (Fabric API, fabric-language-kotlin, mod) are copied to
  `C:\Users\ozzyz\Documents\MineFortress Continued\` with a short "what to try"
  list. Claude does not launch Minecraft on the PC.

## 7. Tracking and git

- GitHub issues on the Lammy93 fork: one per port step and one per bug (with a link to
  the upstream issue), labelled by area (`port`, `bug`, `storage`, `pawns`, `ui`, `combat`).
- Work happens on branches. Merges to `main` only on Zac's go.
- Fixes stay small and self-contained so they can later be offered upstream.

## 8. Done criteria for Phase 1

1. Builds and runs on Fabric 1.21.1 (client and dedicated server).
2. All smoke tests and all bug regression tests pass in the container build.
3. The 1.17.5 test world upgrades cleanly (section 4).
4. Zac has play-tested the final build on his PC and signed off.

## 9. Risks

| Risk | Mitigation |
|---|---|
| The unreleased storage rewrite is half-done | Found at Step 0. Finish it first; it is the planned fix for #30/#36. |
| Automatone port is as large as the mod | Port only what compiles and is reached. Packages MineFortress never touches (e.g. client control/GUI bits) can be cut, as long as tests pass. |
| Fabric-ASM breaks | The flag-based fallback in section 4 is designed up front. |
| 1.20.6 data-component + networking change is large | Converted one packet / item feature at a time, with game-tests after each batch. |
| Mod-compat bugs (#27, #39, #63) need third-party mods to reproduce | A dev run config with Create / Dynmap added, used only for those checks. |
