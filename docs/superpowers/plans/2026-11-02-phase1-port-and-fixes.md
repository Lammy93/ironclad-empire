# MineFortress Continued Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** MineFortress (upstream `master`, Fabric 1.20.2) runs on Fabric 1.21.1 as "MineFortress Continued" 1.18.0, with the 13 open upstream bugs fixed and covered by tests.

**Architecture:** Step-port the existing 7-source-set Gradle project one Minecraft version at a time (1.20.2 → 1.20.4 → 1.20.6 → 1.21.1), building our own fork of Automatone in lock-step as a Gradle included build. A Fabric game-test source set gives a headless smoke + regression suite that must pass at every step. Bug fixes come after the port, each test-first.

**Tech Stack:** Fabric Loom, Fabric API, Yarn mappings, Kotlin (fabric-language-kotlin), Java 17→21, Cardinal Components API, Fabric-ASM (or the fallback), Fabric game-test API, Docker `eclipse-temurin:21-jdk`.

**Spec:** `docs/superpowers/specs/2026-11-02-phase1-port-and-fixes-design.md`

## Global Constraints

- Target: Fabric, Minecraft **1.21.1**, Java 21 (mod targets Java 17 until the 1.20.6 step).
- Mod id stays **`minefortress`**. Display name **"MineFortress Continued"**. Jar base name `minefortress-continued`. Release version **1.18.0**.
- Credits: remmintan (original author) in `fabric.mod.json` `authors` and README. Licence stays MIT.
- Repos: public forks `Lammy93/minefortress` (from upstream `master`) and `Lammy93/automatone` (from `minefortress-mod/automatone`). No publishing to Modrinth/CurseForge.
- Commit identity `LammyK <121271206+Lammy93@users.noreply.github.com>`, messages end with the `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` line. Work on branches; merge to `main` only on Zac's go.
- Builds only run inside Docker via `scripts/gradle.sh`; nothing is installed on the zraid host.
- No Reforged, Elithris or MFAdditions code or assets in Phase 1.
- Never launch Minecraft on lammy-pc; play-test drops go to `C:\Users\ozzyz\Documents\MineFortress Continued\`.
- Every port step is a separate branch + PR: compiles, `runServer` starts, `gametest` green.
- Dependency version numbers in this plan (Loom, Yarn builds, Fabric API, Loader, fabric-language-kotlin) are the intended lines; at each step, confirm the exact newest build for that Minecraft version on fabricmc.net/develop and Modrinth before pinning it, and record the pinned value in `docs/porting-notes.md`.

## Review Focus

1. **Owner offline while pawns work** (dedicated server, owner logs off with tasks queued and pawns in loaded/unloaded chunks): no crash, pawns idle and resume. Test: `OwnerOfflineTest` in Task 9.
2. **Two players with separate fortresses on one server**: actions and storage of one never affect the other. Test: `TwoFortressesIsolationTest` in Task 2.
3. **World saved mid-task, then upgraded to 1.21.1**: the task resumes, or is cancelled with its resources returned to storage, never lost. Covered by the mid-task save in the Task 8 test world.
4. **Mode switching with a full inventory, and death in fortress mode**: no item is ever deleted or duplicated. Test: `ModeSwitchInventoryTest` in Task 11 (full inventory + death cases).
5. **Client without the mod joining a server with it (and the reverse)**: a clean disconnect message, no crash. Manual check in Task 7, step 6.

---

## File Structure

| Path | Responsibility |
|---|---|
| `scripts/gradle.sh` | Runs `./gradlew "$@"` in `eclipse-temurin:21-jdk` with the repo and the `deps/` mounted and a named Gradle cache volume (`mfc-gradle`). |
| `deps/automatone/` | Git submodule → `Lammy93/automatone`; pulled in via `includeBuild` in `settings.gradle`. |
| `src/gametest/` | New source set: `org.minefortress.gametest.*` tests + `FortressTestKit`. Registered via the `fabric-gametest` entrypoint in a separate `fabric.mod.json` under `src/gametest/resources`. |
| `src/gametest/resources/data/minefortress/structures/gametest/*.nbt` | Empty test arenas (`flat_32x32`, `flat_64x64`). |
| `testworlds/fortress-1.17.5.zip` | World made on 1.17.5-beta/1.20.2 for the upgrade check. |
| `docs/porting-notes.md` | Running log of every API change hit per step (name before → after), for upstreaming and the later 26.x port. |
| Existing source sets | Modified in place; no restructuring. |

---

### Task 0: Forks, repo setup, Docker build at 1.20.2

**Files:**
- Create: `scripts/gradle.sh`, `.gitmodules`, `deps/automatone` (submodule), `docs/porting-notes.md`
- Modify: `settings.gradle`, `build.gradle`, `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`, `src/main/resources/fabric.mod.json`, `README.md`

**Interfaces:**
- Produces: `scripts/gradle.sh <gradle args>` (exit code = Gradle's); `included build` coordinates `com.github.minefortress-mod:automatone` substituted by `deps/automatone`.

- [ ] **Step 1:** Create the forks under the personal Lammy93 account: `gh repo fork remmintan/minefortress --clone=false` and `gh repo fork minefortress-mod/automatone --clone=false`. In the local repo: `git remote add origin https://github.com/Lammy93/minefortress.git`, push `master` as `main` plus the existing `docs/phase1-spec` branch. Create labels `port`, `bug`, `storage`, `pawns`, `ui`, `combat`, plus one issue per Task 1–19 linking the upstream issue numbers.
- [ ] **Step 2:** Write `scripts/gradle.sh` (bash, `set -euo pipefail`, `docker run --rm -v "$REPO:/w" -v mfc-gradle:/root/.gradle -w /w eclipse-temurin:21-jdk ./gradlew --no-daemon "$@"`).
- [ ] **Step 3:** Bump the Gradle wrapper to **8.10.2** and Loom to **1.7.4** (newest that still supports 1.20.2 with Yarn), Kotlin plugin to **2.0.21**, fabric-language-kotlin `1.12.3+kotlin.2.0.21`. Keep `release.set(17)` and `jvmToolchain(17)` → change the toolchain to compile with 21 but `release 17`.
- [ ] **Step 4:** Add the Automatone submodule at `deps/automatone`. In `settings.gradle`, `includeBuild('deps/automatone') { dependencySubstitution { substitute module('com.github.minefortress-mod:automatone') using project(':') } }`. Apply the same Gradle/Loom bumps inside the submodule (commit there first).
- [ ] **Step 5:** Branding: `fabric.mod.json` `name` → "MineFortress Continued", `authors` → `["remmintan", "MineFortress Continued contributors"]`, `contact.sources` → fork URL; `archives_base_name=minefortress-continued`; README gets a top section saying this is a continued fork, with a link to upstream.
- [ ] **Step 6: Verify** `scripts/gradle.sh build` → `BUILD SUCCESSFUL`, and `build/libs/minefortress-continued-dev.jar` exists.
- [ ] **Step 7: Verify** `scripts/gradle.sh runServer --args=--nogui` with `eula=true` in `run/` reaches `Done (` in the log within 3 minutes, then stop it.
- [ ] **Step 8: Commit + PR** `build: Docker build, Automatone source build, branding (1.20.2 baseline)`.

### Task 1: Game-test harness and smoke suite (still 1.20.2)

**Files:**
- Create: `src/gametest/java/org/minefortress/gametest/FortressTestKit.kt`, `SmokeTests.kt`, `src/gametest/resources/fabric.mod.json`, the two arena `.nbt` files
- Modify: `build.gradle` (source set `gametest`, run config `gametest` with `-Dfabric-api.gametest` and `-Dfabric-api.gametest.report-file=build/gametest.xml`)

**Interfaces:**
- Produces:
  - `object FortressTestKit { fun createFortress(ctx: TestContext, center: BlockPos = BlockPos(16, 1, 16)): FortressFixture }`
  - `data class FortressFixture(val player: ServerPlayerEntity /* Fabric FakePlayer */, val fortressManager: IServerFortressManager, val managers: IServerManagersProvider, val center: BlockPos)`
  - `fun FortressFixture.spawnPawns(n: Int): List<BasePawnEntity>`, `fun FortressFixture.give(item: Item, count: Int)` (into fortress storage), `fun FortressFixture.storageCount(item: Item): Int`, `fun FortressFixture.saveAndReload(ctx: TestContext): FortressFixture` (writes manager NBT, rebuilds the managers from it)
  - Gradle task `runGametest`; the report is at `build/gametest.xml`.

- [ ] **Step 1:** Write `SmokeTests` with `@GameTest(templateName = "minefortress:gametest/flat_64x64", tickLimit = 6000)` tests: `fortressSetupCreatesCampfire` (campfire block at `center`, 5 starting pawns), `pawnBuildsSmallBlueprint` (`"small_house_1"` or the smallest predefined blueprint from `predefined_blueprints.json`; all blocks placed), `pawnsCutTree` (place an oak tree; `CutTreesTask` leaves 0 logs, logs in storage), `warriorKillsZombie` (zombie dead within 1200 ticks), `stateSurvivesSaveReload` (pawn count, storage counts and profession counts equal after `saveAndReload`).
- [ ] **Step 2:** Add `TwoFortressesIsolationTest` (Review Focus 2): two fixtures 40 blocks apart; giving 10 cobblestone to A leaves B's `storageCount` at 0; a task issued by A never takes B's pawns.
- [ ] **Step 3:** Run `scripts/gradle.sh runGametest` → expected FAIL: `FortressTestKit` is not implemented.
- [ ] **Step 4:** Implement `FortressTestKit`, using the same server calls that `C2SSetupCampfirePacket` and the fortress creation path make (no client packets). `player` = `FakePlayer.get(ctx.world, GameProfile(UUID, "mfc_test_N"))`.
- [ ] **Step 5:** Run `runGametest` → all PASS. Any failure here is an existing upstream bug: log it in an issue, mark the test `@Disabled`-equivalent (`required = false`), and note it in the Task 2 decision.
- [ ] **Step 6: Commit + PR** `test: Fabric game-test harness and smoke suite`.

### Task 2: Storage-rewrite audit and decision gate (1.20.2)

**Files:**
- Create: `src/gametest/java/org/minefortress/gametest/StorageTests.kt`, `docs/storage-audit.md`
- Modify (only if completing the rewrite): `src/main/java/org/minefortress/fortress/resources/**`, `src/core/.../interfaces/resources/**`

**Interfaces:**
- Consumes: `FortressFixture.give/storageCount`.
- Produces: the resource API that later tasks rely on, recorded in `docs/storage-audit.md` (the exact class + method names for "add to storage", "take from storage", "craft").

- [ ] **Step 1:** Write `StorageTests`: `itemsLiveInRealChests` (after `give(COBBLESTONE, 64)` a chest block entity linked to the fortress holds 64), `taskReservesAndConsumes` (blueprint task: storage drops by exactly the blueprint's block count on completion), `cancelReturnsResources` (cancel mid-task: storage back to the original count), `noLossOnSaveMidTask` (save/reload mid-task, then cancel: original count).
- [ ] **Step 2:** Run → record PASS/FAIL per test in `docs/storage-audit.md`, plus a 15-minute manual dev-client session (`scripts/gradle.sh runClient` is not available headless, so this is done via a play-test drop to the PC: build the jar, copy a 1.20.2 instance to the drop folder, Zac plays) noting broken screens.
- [ ] **Step 3: Decision gate:** if all pass and the play-test is OK, the rewrite is accepted. Otherwise, add sub-tasks to the plan (one per failing behaviour, same test-first pattern) and complete them before Task 3. Get Zac's ok on the decision.
- [ ] **Step 4: Commit + PR** `test: storage rewrite audit`.

### Task 3: Port to 1.20.4 + Fabric-ASM check

**Files:**
- Modify: `gradle.properties` (`minecraft_version=1.20.4`, `yarn_mappings=1.20.4+build.3`, `loader_version=0.16.9`, `fabric_version=0.97.2+1.20.4`), `src/**` as compile errors require, `deps/automatone/**` likewise, `docs/porting-notes.md`

- [ ] **Step 1:** Bump versions in both builds and run `scripts/gradle.sh build`. Fix compile errors file by file, logging each API rename in `porting-notes.md`.
- [ ] **Step 2: Verify** `runGametest` → all tests that passed in Task 1/2 still PASS.
- [ ] **Step 3:** Add game test `GameModeTests.fortressModeRoundTrip`: `player.changeGameMode(GameMode.FORTRESS)`, assert `isFortressGamemode(player)` (top-level function in `FortressGamemodeUtils.kt`), `changeGameMode(SURVIVAL)`, assert false; and `fortressModePersists`: after save/reload, still fortress.
- [ ] **Step 4:** Run → if PASS, Fabric-ASM works: note this in `porting-notes.md` and skip Task 3b. If the game crashes at load or the tests fail on ASM, do Task 3b before Task 4.
- [ ] **Step 5: Commit + PR** `port: Minecraft 1.20.4`.

### Task 3b (conditional): Fortress-mode flag replaces the injected game mode

**Files:**
- Create: `src/core/java/net/remmintan/mods/minefortress/core/components/FortressModeComponent.kt`, `src/main/java/org/minefortress/commands/FortressCommand.kt`
- Modify: `FortressGamemodeUtils.kt` (the single source of truth), every `GameMode.FORTRESS` / `"fortress"` reference (132; route them all through `FortressGamemodeUtils`), `fabric.mod.json` (remove the `preLaunch`, `mm:early_risers` entrypoints and the Fabric-ASM jar), delete `earlyriser/GameModeEarlyRiser.java`

**Interfaces:**
- Produces: the existing top-level `isFortressGamemode(player: PlayerEntity?)`, `isFortressGamemode(livingEntity: LivingEntity?)`, `isClientInFortressGamemode()` keep their signatures (now read the flag), plus new `setFortressGamemode(player: ServerPlayerEntity, on: Boolean)` in `FortressGamemodeUtils.kt`; CCA key `minefortress:fortress_mode` (boolean, synced to the owning client); command `/fortress [on|off]`; keybind `key.minefortress.toggle_mode` (default unbound).

- [ ] **Step 1:** Make the Task 3 `GameModeTests` use `setFortressGamemode` instead of `changeGameMode(GameMode.FORTRESS)`; add `legacyGameModeIdMigrates`: player NBT with `playerGameType: 4` loads as survival + fortress flag on.
- [ ] **Step 2:** Run → FAIL (functions missing).
- [ ] **Step 3:** Implement the component + utils; underlying game mode = creative if the fortress uses "creative fortress" settings, else survival (the same rule the current code uses for its creative/survival fortress variants). Migrate legacy id 4 in a mixin on `ServerPlayerEntity.readCustomDataFromNbt`.
- [ ] **Step 4:** Run `runGametest` → all PASS. Manual: F3+F4 switcher shows no Fortress entry and doesn't crash.
- [ ] **Step 5: Commit + PR** `refactor: fortress mode as player flag (Fabric-ASM removed)`.

### Task 4: Port to 1.20.6 (data components, typed payloads, Java 21)

**Files:**
- Modify: `gradle.properties` (`minecraft_version=1.20.6`, `yarn_mappings=1.20.6+build.3`, `fabric_version=0.100.8+1.20.6`), `build.gradle` (`release 21`, `jvmToolchain(21)`), all 36 classes under `src/networking/**/c2s|s2c`, `NetworkingReadersRegistry`, `ServerNetworkReceivers`, `FortressClientNetworkHelper`, `FortressServerNetworkHelper`, the 48 files that touch `NbtCompound` on items, `deps/automatone/**`

**Interfaces:**
- Produces: every packet is a `record`/`data class` implementing `CustomPayload` with `companion/static val ID: CustomPayload.Id<...>` and `val CODEC: PacketCodec<RegistryByteBuf, ...>`; registered in `PayloadTypeRegistry.playC2S()/playS2C()` in one place: `NetworkingReadersRegistry.registerAll()`. `FortressClientNetworkHelper.send(payload: CustomPayload)` and `FortressServerNetworkHelper.send(player, payload)` replace the channel+buf overloads.

- [ ] **Step 1:** Bump versions; switch to Java 21. Build → collect the error list into `porting-notes.md`.
- [ ] **Step 2:** Convert packets in batches of ~6 (grouped by feature: blueprints, tasks, professions, buildings, resources, misc), building after each batch. Keep the wire fields identical in order and type.
- [ ] **Step 3:** Convert item NBT uses to data components (`DataComponentTypes.CUSTOM_DATA` for mod-private item data; vanilla components where one exists, e.g. names/lore).
- [ ] **Step 4:** Add the game test `NetworkRoundTripTests.everyPayloadRoundTrips`: for each registered payload type with a sample instance (a `samples()` list in the test), encode then decode with its codec, and assert equals.
- [ ] **Step 5: Verify** `runGametest` → all PASS; `runServer` reaches `Done (`.
- [ ] **Step 6: Commit + PR** `port: Minecraft 1.20.6 (payloads, data components, Java 21)`.

### Task 5: Port to 1.21.1 + official Cardinal Components 6.1.3

**Files:**
- Modify: `gradle.properties` (`minecraft_version=1.21.1`, `yarn_mappings=1.21.1+build.3`, `fabric_version=0.115.1+1.21.1`), `build.gradle` (replace the three `minefortress-cc-api` deps with `org.ladysnake.cardinal-components-api:cardinal-components-{base,entity,world}:6.1.3` from `https://maven.ladysnake.org/releases`), every `new Identifier(` → `Identifier.of(`, the CCA imports `dev.onyxstudios.cca` → `org.ladysnake.cca`, `src/main/resources/data/minefortress/**` folder renames (plural → singular: `structures`→`structure`, `recipes`→`recipe`, `tags/blocks`→`tags/block`, etc.), `deps/automatone/**`

- [ ] **Step 1:** Diff `minefortress-mod/minefortress-cc-api` against upstream CCA 5.2.9; list its patches in `porting-notes.md`. If a patch is needed by MineFortress, re-apply it as a mixin in our code; otherwise drop it.
- [ ] **Step 2:** Bump versions, swap CCA, build, fix errors, rename data folders.
- [ ] **Step 3: Verify** `runGametest` → all PASS (including the Task 4 round-trips); `runServer` reaches `Done (` with no `Couldn't load` or `Unknown registry key` lines in the log.
- [ ] **Step 4: Commit + PR** `port: Minecraft 1.21.1`.

### Task 6: Play-test drop tooling

**Files:**
- Create: `scripts/playtest-drop.sh`

**Interfaces:**
- Produces: `scripts/playtest-drop.sh <label>`: builds, assembles a folder `MFC-<label>/` with `mods/` (our jar, Fabric API 0.115.1+1.21.1, fabric-language-kotlin 1.12.3+kotlin.2.0.21, all downloaded from Modrinth by exact version), `README.txt` (install steps for the Modrinth app / Prism: Fabric loader 0.16.9 on 1.21.1, copy `mods/`), and `WHAT-TO-TRY.txt` (passed in from `docs/playtest/<label>.md`), then `scp`s it to `lammy-pc:"C:/Users/ozzyz/Documents/MineFortress Continued/"`.

- [ ] **Step 1:** Write the script; dry-run flag `-n` lists files without copying.
- [ ] **Step 2: Verify** `scripts/playtest-drop.sh -n port-1.21.1` lists the 3 jars + 2 txt files.
- [ ] **Step 3:** Real drop with `docs/playtest/port-1.21.1.md` (set up a fortress, build 2 blueprints, hire 2 professions, chop, fight at night, save/quit/reload). Tell Zac where it is and wait for his feedback; file any problems as issues.
- [ ] **Step 4: Commit** `build: play-test drop script`.

### Task 7: Client/server mismatch handling

**Files:**
- Modify: `src/main/java/org/minefortress/MineFortressMod.java` (or wherever the server networking is set up) — a server-side check on join

- [ ] **Step 1:** Manual repro on the dev server: join with a vanilla 1.21.1 client. Record what happens.
- [ ] **Step 2:** If it isn't a clean disconnect, use Fabric's `ServerConfigurationConnectionEvents` + `ServerConfigurationNetworking.canSend(handler, <any minefortress payload id>)` to disconnect with `Text.literal("MineFortress Continued 1.18.0 is required on the client to join this server.")`.
- [ ] **Step 3:** Repeat the repro → expect that message.
- [ ] **Step 4:** Reverse case: a modded client on a vanilla server can play vanilla survival with no crash (the mod's client code must not send payloads the server can't accept; guard with `ClientPlayNetworking.canSend`).
- [ ] **Step 5: Commit + PR** `fix: clean handling of client/server mod mismatch`, with both repro results (Review Focus 5) in the PR description.

### Task 8: World upgrade check

**Files:**
- Create: `testworlds/fortress-1.17.5.zip`, `docs/playtest/world-upgrade.md`

- [ ] **Step 1:** Make the 1.17.5 world: either Zac plays the original 1.17.5-beta on 1.20.2 for ~20 min (fortress, ≥3 professions hired, ≥2 buildings, storage filled, 1 custom blueprint saved, **1 blueprint task left half-built when quitting**), or build it with the Task 0 1.20.2 jar via the dev server + scripted commands. Zip it to `testworlds/` (keep it under 20 MB).
- [ ] **Step 2:** Add game test `WorldUpgradeTest` (runs only when `-Dmfc.upgradeWorld=testworlds/fortress-1.17.5.zip`): starts the dedicated server on an unzipped copy, waits for load, asserts pawn count, profession counts, building count and storage totals equal the values recorded in `docs/playtest/world-upgrade.md`, and that the half-built task either resumes or its resources are back in storage (Review Focus 3).
- [ ] **Step 3:** Run → fix any data-fixer gaps (old item NBT → components for mod-private data needs a `DataFixer`-style read fallback in the components' readers).
- [ ] **Step 4: Commit + PR** `test: 1.17.5 world upgrade`.

### Tasks 9–18b: Bug fixes

Each bug task follows this pattern: write the test → run it on the 1.21.1 build → **if it already passes, record "fixed upstream / by the port" on the issue and close it with the test kept** → otherwise fix → test passes → commit + PR referencing `Fixes Lammy93/minefortress#<n> (upstream remmintan/minefortress#<m>)`.

### Task 9: #26 server crash when the owner is offline

**Files:** Test `src/gametest/.../OwnerOfflineTest.kt`; modify `src/main/java/org/minefortress/tasks/ServerTaskManager.kt:60-84` if needed.

- [ ] **Step 1:** `OwnerOfflineTest.tasksPauseWhileOwnerOffline`: queue a 2-part task, remove the fake player from the server, tick 2400 ticks → no exception; pawns idle; re-add the player → the task completes within 3000 ticks.
- [ ] **Step 2:** `pawnsInUnloadedChunkWhileOffline` (Review Focus 1): as above, with one pawn force-unloaded (`world.getChunkManager().setChunkForced(false)` on its chunk) → no exception, and the pawn rejoins its task after reload.
- [ ] **Step 3:** Run; if it fails, make offline-owner tasks wait without dereferencing the player (the `FIXME` at line 63 wants "take planned tasks even when the player is offline": leave that behaviour out of Phase 1, only guarantee no crash). Commit + PR.

### Task 10: #63/#39 crash when hovering buttons with other mods

**Files:** Modify `src/main/java/org/minefortress/mixins/entity/player/FortressClientPlayerEntityMixin.java:38-55` and every `crosshairTarget` consumer that calls `getBlockPos()` (`BaseClientStructureManager.java:252`, `FortressGameRendererMixin.java:72`, `FortressWorldRendererMixin.java:77`, `AreasClientManager.java:70`, `SelectionManager.java:206`).

- [ ] **Step 1:** Repro: dev client run config `runClientCompat` with Create (Fabric 1.21.1 build) in `run/mods-compat/`. Hover the pawns button → record the stack trace (the expected NPE is `BlockPos` null).
- [ ] **Step 2:** Every consumer treats `HitResult.Type.MISS` or a null `BlockPos` as "nothing hovered" and returns early; the raycast override never returns a `BlockHitResult` with a null pos (return `BlockHitResult.createMissed(...)` instead).
- [ ] **Step 3:** Repeat the repro with Create → no crash for 2 minutes of hovering every HUD button. Also check Botania if it has a 1.21.1 Fabric build.
- [ ] **Step 4:** Commit + PR.

### Task 11: #49 inventory wiped on "Explore your village!"

**Files:** Create `src/core/java/net/remmintan/mods/minefortress/core/components/SurvivalInventoryComponent.kt`; modify `src/networking/.../c2s/C2SSwitchToMinecraftSurvival.kt:24` (remove `player.inventory.clear()`), the fortress-mode entry path (`FortressGamemodeUtils` set-fortress), `ServerboundFinishEditBlueprintPacket.java:36` (same clear, same treatment); test `ModeSwitchInventoryTest.kt`.

**Interfaces:**
- Produces: CCA key `minefortress:survival_inventory` storing the player's `PlayerInventory` as NBT; `SurvivalInventoryComponent.stash(player)`, `.restore(player): Boolean`.

- [ ] **Step 1:** `ModeSwitchInventoryTest`: `survivalItemsComeBack` (give 32 diamonds + an enchanted sword in survival; enter fortress; "explore" → the survival inventory equals the original exactly); `fullInventoryRoundTrip` (all 41 slots full, including armour + offhand; round-trip; equal); `deathInFortressModeKeepsStash` (kill the player in fortress mode; respawn; explore → stash restored, nothing dropped twice); `blueprintEditorKeepsInventory` (enter + finish the blueprint editor → inventory equal).
- [ ] **Step 2:** Run → FAIL on `survivalItemsComeBack`.
- [ ] **Step 3:** Stash on entering fortress mode / the blueprint editor; restore on "explore" / editor finish. Restore only into an empty inventory; anything already there (fortress-mode items) goes to fortress storage first.
- [ ] **Step 4:** Run → PASS. Commit + PR.

### Task 12: #30 + #36 item loss and duplication in crafting/stacking

**Files:** Test `CraftingTests.kt`; modify the crafting handler named in `docs/storage-audit.md`, plus the crafting screen in `src/gui/**` if it still sends `ClickSlotC2SPacket` for crafting.

**Interfaces:**
- Produces: payload `C2SCraftRequest(recipeId: Identifier, times: Int)`, handled server-side as one storage transaction (take ingredients × times, add result × times, all-or-nothing).

- [ ] **Step 1:** `CraftingTests`: `craftTwoUsesExactlyTwice` (storage 8 planks; craft crafting table ×2 → 2 tables, 0 planks); `craftWithInsufficientIsNoop` (3 planks; craft ×1 table → nothing changes); `hotbarFillStackDropConserves` (fill the hotbar from storage, stack slots, shift-click, Q-drop → total items across storage + inventory + dropped entities is unchanged).
- [ ] **Step 2:** Run → record which fail.
- [ ] **Step 3:** Route crafting through `C2SCraftRequest`; remove any client-side slot manipulation for crafting.
- [ ] **Step 4:** Run → PASS. Manual check on a dedicated server with 2 crafts' worth of ingredients: no duplication. Commit + PR.

### Task 13: #51 + #50 pawns lose professions

**Files:** Test `ProfessionPersistenceTests.kt`; modify `src/main/java/org/minefortress/professions/ServerProfessionManager.java` (`read` at line 150, `tick` at 90, `decreaseAmount` at 77), the blueprint-editor finish path (`ServerboundFinishEditBlueprintPacket.java`).

**Interfaces:**
- Produces: `ServerProfessionManager.rebuildFromPawns(pawns: Collection<IProfessional>)`, the profession count = number of loaded + saved pawns with that profession id; pawn → profession assignment is keyed by pawn UUID in the manager's NBT (`"assignments": {uuid: professionId}`).

- [ ] **Step 1:** Tests: `professionsSurviveBlueprintEdit` (hire 2 miners; enter + finish the blueprint editor → still 2 miners working, the menu count is 2); `unloadedPawnKeepsJob` (hire 1 farmer; unload its chunk; hire 1 lumberjack; reload → farmer still a farmer); `saveReloadKeepsAssignments`.
- [ ] **Step 2:** Run → FAIL expected on the first two.
- [ ] **Step 3:** Persist assignments by UUID; call `rebuildFromPawns` after the editor finishes and on world load; `decreaseAmount` never picks an unloaded pawn's assignment.
- [ ] **Step 4:** Run → PASS. Commit + PR.

### Task 14: #48 campfire destroyed → softlock

**Files:** Test `CampfireTests.kt`; modify the dig/clear task block filter (`SimpleSelectionTask.kt`, `CutTreesTask.kt`, `AreaBlueprintTask.kt`: wherever blocks to break are collected), `CampfireFunctions.kt` / the HUD (rebuild action), `PlaceCampfireTask.kt` (reuse it for the rebuild).

- [ ] **Step 1:** Tests: `digTaskSkipsCampfire` (a dig selection covering the campfire → the campfire is still there after the task); `rebuildCampfireWhenMissing` (set the campfire block to air; invoke the rebuild action → the campfire is back at `center` and the fortress works).
- [ ] **Step 2:** Run → FAIL.
- [ ] **Step 3:** Exclude the fortress centre block from every pawn break list; add a "Rebuild campfire" button, shown only when the centre block isn't a `FortressCampfireBlock`, that queues `PlaceCampfireTask` at `center` (free of cost).
- [ ] **Step 4:** Run → PASS. Commit + PR.

### Task 15: #27 blueprint editor with Dynmap

**Files:** Modify `src/core/.../blueprints/world/BlueprintsDimensionUtils.kt` and the editor world creation it drives.

- [ ] **Step 1:** Repro with Dynmap (Fabric 1.21.1) in `run/mods-compat/`: on a dedicated server, create a blueprint → record the error.
- [ ] **Step 2:** Make the editor dimension a data-pack dimension (`data/minefortress/dimension/blueprints.json`, which already exists under `dimension/`) loaded by the server at start-up, so `ServerWorldEvents.LOAD` fires for it like any other world; remove any runtime world construction.
- [ ] **Step 3:** Repeat the repro → editor opens, saves, and Dynmap logs no errors. `runGametest` still PASS (blueprint smoke test). Commit + PR.

### Task 16: #33 pawns stuck in table blocks

**Files:** Test `PawnUnstuckTests.kt`; modify the blueprint placement order (where `AreaBlueprintTask`/`TaskPart` sorts block positions) and the pawn's post-place logic in `BasePawnEntity.java` or its building controller.

**Interfaces:**
- Produces: `BlueprintBlockOrder.comparator(): Comparator<BlockInfo>`: full solid blocks first (by y then distance), then non-full blocks (fences, walls, pressure plates, carpets, slabs, doors, torches, beds).

- [ ] **Step 1:** Tests: `tableBuiltLast` (blueprint with a fence + pressure plate table → in placement order these come after all full blocks); `pawnLeavesNonFullBlock` (teleport a pawn into a fence position, then place the fence → within 40 ticks the pawn's bounding box doesn't intersect it).
- [ ] **Step 2:** Run → FAIL.
- [ ] **Step 3:** Implement the comparator; after each placement, if the pawn's box intersects the placed shape, move it to the nearest standable position within 3 blocks.
- [ ] **Step 4:** Run → PASS. Commit + PR.

### Task 17: #29 idle army doesn't fight back

**Files:** Test `ArmyRetaliationTests.kt`; modify `WarriorPawn.java`, `ArcherPawn.java` (their goal selectors).

- [ ] **Step 1:** Tests: `idleWarriorRetaliates` (zombie attacks an idle warrior → the warrior targets the zombie within 20 ticks; the zombie is dead within 600); `idleArcherRetaliates` (skeleton at 10 blocks shoots an archer → the archer fires back); `orderedMoveIsNotInterrupted` (warrior under an explicit move order keeps moving when hit and only retaliates after arriving).
- [ ] **Step 2:** Run → FAIL on the first two.
- [ ] **Step 3:** Add `RevengeGoal` (vanilla) at a priority below explicit orders; idle soldiers also target hostiles within 8 blocks of themselves.
- [ ] **Step 4:** Run → PASS. Commit + PR.

### Task 18: #45 outdated-world message

**Files:** Modify `src/gui/java/net/remmintan/mods/minefortress/gui/OutdatedWorldScreen.kt:9-12`.

- [ ] **Step 1:** Replace the text with exactly: title `"This world was made with a very old MineFortress"`, body `"Worlds from MineFortress versions before the fortress rework can't be opened with this version. Your world has not been changed. To keep playing it, use the MineFortress version it was made with; to play MineFortress Continued, create a new world."`
- [ ] **Step 2: Verify** build + a manual look at the screen via a play-test drop (or a dev client if available). Commit + PR.

### Task 18b: Server-side ownership checks (added 2026-11-03, Zac's go)

**Files:** Create `src/main/java/org/minefortress/fortress/FortressOwnership.kt`; test `OwnershipTests.kt`; modify the `handle` of `C2SDestroyBuilding`, `C2SRepairBuilding`, `C2SHireProfessional`, `C2SOpenBuildingScreen`, `C2SMoveTargetPacket`, `C2SFollowTargetPacket`, `ServerboundSimpleSelectionTaskPacket`, `ServerboundBlueprintTaskPacket`, `ServerboundCutTreesTaskPacket`, `ServerboundRoadsTaskPacket`; `FortressCampfireBlock` (break handling).

**Interfaces:**
- Produces: `object FortressOwnership { fun ownsPawn(player: ServerPlayerEntity, entity: Entity?): Boolean; fun ownsBuilding(player: ServerPlayerEntity, pos: BlockPos): Boolean; fun ownsCampfire(player: ServerPlayerEntity, pos: BlockPos): Boolean }`. Ownership = the target's `fortressPos` equals the sender's `(player as IFortressPlayerEntity).get_FortressPos()`; operators (permission level 2+) always pass. A rejected packet is dropped with a `LOGGER.warn` naming the player and packet.

- [ ] **Step 1:** `OwnershipTests` (two fixtures A and B): `cannotDestroyOthersBuilding` (B sends the destroy packet for A's building → building intact); `cannotHireInOthersFortress` (B hires in A's building → A's storage and profession counts unchanged); `cannotCommandOthersSoldier` (B sends move/follow for A's warrior → its target unchanged); `cannotTaskOthersArea` (B digs inside A's fortress bounds → no blocks removed); `campfireOnlyBreakableByOwner` (a survival non-owner fails to break A's campfire; the owner can).
- [ ] **Step 2:** Run → FAIL (every exploit currently succeeds).
- [ ] **Step 3:** Implement `FortressOwnership` and call it at the top of each `handle`; stop non-owners breaking the campfire (`PlayerBlockBreakEvents.BEFORE` → false unless `ownsCampfire`).
- [ ] **Step 4:** Run → PASS; whole suite green. Commit + PR referencing the issue.

### Task 19: Release 1.18.0

**Files:** Modify `gradle.properties` (`mod_version=1.18.0`), create `CHANGELOG.md`.

- [ ] **Step 1:** `CHANGELOG.md` section "1.18.0 — MineFortress Continued": the port, each bug with its upstream number, the fortress-mode change if Task 3b ran.
- [ ] **Step 2: Verify** a clean `scripts/gradle.sh clean build runGametest` → all PASS; `runServer` reaches `Done (`; the `WorldUpgradeTest` passes.
- [ ] **Step 3:** `scripts/playtest-drop.sh 1.18.0` with `docs/playtest/1.18.0.md` covering every fixed bug's repro steps. Wait for Zac's sign-off (done criterion 4).
- [ ] **Step 4:** On Zac's go: merge to `main`, tag `v1.18.0`, and add a GitHub release with the jar (no Modrinth/CurseForge).
