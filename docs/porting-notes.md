# Porting notes

Running log of every change made to get MineFortress building on each Minecraft version.
Kept for upstreaming fixes and for the later 26.x port.

## Step 0: tooling (still Minecraft 1.20.2)

| What | Before | After | Why |
|---|---|---|---|
| Gradle wrapper | 8.3 | 8.10.2 | Gradle 8.3 cannot run on JDK 21 |
| Fabric Loom | 1.4-SNAPSHOT | 1.7.4 | Needs Gradle 8.8+; works on JDK 21 |
| Kotlin Gradle plugin | 1.9.21 | 2.0.21 | Matches fabric-language-kotlin 1.12.3+kotlin.2.0.21 |
| fabric-language-kotlin | 1.10.20+kotlin.1.9.24 | 1.12.3+kotlin.2.0.21 | |
| foojay toolchain resolver | 0.5.0 | 0.8.0 | 0.5.0 drags Gson 2.9.1 onto the build classpath, which breaks Loom (record deserialisation) |
| Java toolchain | `jvmToolchain(17)` | compile on JDK 21, `jvmTarget`/`release` 17 | No separate JDK 17 download needed |
| Publishing | minotaur + cursegradle plugins, nightly/release workflows | removed | Gson conflict; this fork does not publish to Modrinth/CurseForge |
| Automatone | JitPack `com.github.minefortress-mod:automatone:1.0.9-minefortress` | submodule `deps/automatone` (Lammy93/automatone, branch `mfc`), published to `deps/m2` as `io.github.ladysnake:automatone:1.0.9-mfc` by `scripts/build-deps.sh` | Built from source so it can be ported alongside |
| Automatone Loom | 1.2.7 | 1.7.4 | Same Gradle as the main build |
| Automatone plugin repos | Cotton maven (server.bbkr.space) | removed | Dead repository, not needed |

### Upstream bug found: dedicated server crash on start (since unreleased commit b0ca1779, 2025-07-08)

`MineFortressMod.onInitialize` (common entry point) called `BuildingsHudRenderer.register()` and
`FortressBlocks.register()` registered a `BlockEntityRendererFactories` renderer. Both are client-only
classes, so a dedicated server failed with `Cannot load class ...BlockEntityRenderer in environment type SERVER`.
Fix: both registrations moved to `MineFortressClient.onInitializeClient`.

## Step 1: Minecraft 1.20.2 → 1.20.4

Pinned: Yarn `1.20.4+build.3`, Fabric API `0.97.3+1.20.4`, Fabric Loader `0.16.14`. Automatone: same bump (branch `mfc`).

| Change | Before | After | Where |
|---|---|---|---|
| Grass item/block | `Items.GRASS` / `Blocks.GRASS` | `Items.SHORT_GRASS` / `Blocks.SHORT_GRASS` | SimilarItemsHelper, ForesterDailyTask, FarmAreaIterator |
| Compressed NBT read | `NbtIo.readCompressed(stream)` | `NbtIo.readCompressed(stream, NbtSizeTracker.ofUnlimitedBytes())` | ServerboundBlueprintsImportExportPacket; Automatone schematic formats |
| Widget drawing | override `render` / `renderButton` | override `renderWidget` (`render` is final) | PawnSkinButton, Text/DynamicText/ItemButtonWidget |
| Widget tooltip position | `getTooltipPositioner()` | `HoveredTooltipPositioner.INSTANCE` | same widgets (check in play-test: 1.20.3 also auto-draws `setTooltip` tooltips, so they may show twice) |
| List widgets | `ctor(client, w, h, top, bottom, itemH)`, `setLeftPos`, `right` | `ctor(client, w, bottom-top, top, itemH)`, `setX`, `getRight()` | BlueprintsListWidget, ImportExportBlueprintsScreen |
| `BlockWithEntity` | — | must implement `getCodec()` (`MapCodec.unit`) | 5 fortress blocks |
| `LevelProperties.readProperties` mixin | `(Dynamic, DataFixer, int, NbtCompound, LevelInfo, SaveVersionInfo, SpecialProperty, GeneratorOptions, Lifecycle)` | `(Dynamic, LevelInfo, SpecialProperty, GeneratorOptions, Lifecycle)` | LevelPropertiesMixin |
| `SimpleInventory` field | `stacks` (public) | `heldStacks` (private) | FortressSimpleInventoryMixin |
| `IntegratedServerLoader.start` mixin | `start(Screen, String, boolean, boolean)` | private `start(Session, Dynamic, boolean, boolean, Runnable)`; modify arg at HEAD | FortressIntegratedServerLoaderMixin |
| Automatone `Box` | `new Box(BlockPos, BlockPos)` | `new Box(Vec3d.of(a), Vec3d.of(b))` | SelCommand, Selection, SelectionRenderer |
| Automatone `EntityMixin` | injected into `hasPlayerRider` lambda `method_31469` | `@Inject` RETURN of `getPlayerPassengers`, minus fake players | EntityMixin |

Build/infra fixes found at this step:
- `fabric.mod.json` pinned `"minecraft": "1.20.2"`; now `${minecraft_version}` / `${fabric_api_min}` from `gradle.properties`.
- Loom caches remapped mod jars by coordinates: `scripts/build-deps.sh` now deletes the cached Automatone copy.
- `build-deps.sh` no longer runs `git submodule update` on an existing checkout (it detached the submodule onto an older commit).
- `scripts/gametest.sh` deletes the old report and fails without a fresh one.
- Mixin target problems show up at compile time as `warning: Cannot find target ...` in the build log: check for these after every bump.

**Fabric-ASM check:** `GameModeTests` (fortress game mode round trip + saved id) pass on Loader 0.16.14 → Fabric-ASM still works; Task 3b (player-flag fallback) not needed.

Result: build OK, `runServer` reaches `Done (17.8s)`, game tests 13/13.

## Step 2: Minecraft 1.20.4 → 1.20.6 (Java 21)

Pinned: Yarn `1.20.6+build.3`, Fabric API `0.100.8+1.20.6`, Loader `0.16.14`, Cardinal Components API **6.0.0** (official; moved here from Step 3 because the `5.2.9-minefortress` fork crashes on 1.20.6). The fork only carried build/publishing changes, no gameplay patches, so nothing had to be re-applied.

**Networking (`FortressRawPayload`).** 1.20.5 requires typed `CustomPayload`s. Instead of rewriting 36 packet classes, every MineFortress channel is registered as its own payload type whose body is exactly the bytes the packet class writes (`write(buf)` / `(buf)` constructor unchanged, wire format unchanged). Buffers are `RegistryByteBuf`s. S2C payload types are listed in `FortressRawPayload.S2C_CHANNELS` (registered from common code); C2S types are registered by `ServerNetworkReceivers.registerReceiver`. Packets can later move to typed payloads one at a time.

**Items in packets (`PacketCompat`).** `PacketByteBuf.writeItemStack/readItemStack` and `ItemVariant.toPacket/fromPacket` are gone. `PacketCompat` keeps the old `FortressPacketByteBufMixin` format (VarInt count, so totals above 64 survive; they come back as `FortressItemStack`), with data-component changes instead of NBT. The mixin was removed; `FortressItemStack` moved to `core` (same package).

| Change | Before | After | Where |
|---|---|---|---|
| Entity data tracker | `initDataTracker()` + `dataTracker.startTracking` | `initDataTracker(DataTracker.Builder)` + `builder.add` | 7 entities + FortressPlayerEntityMixin |
| Enum tracked data | `TrackedDataHandler.ofEnum` | `TrackedDataHandler.create(PacketCodecs.indexed(...))` | MineFortressMod |
| Spawn NBT | `MobEntity.initialize(..., NbtCompound)` / `EntityType.spawn(world, nbt, ...)` | `BasePawnEntity.applySpawnData(nbt)` called from the spawn consumer (`applySpawnDataTo`) | BasePawnEntity, NamedPawnEntity, WarriorPawn, ServerFortressManager, test kit |
| Block entity NBT | `writeNbt/readNbt(nbt)` | `(nbt, RegistryWrapper.WrapperLookup)` | 5 fortress block entities |
| Block use | `onUse(state, world, pos, player, hand, hit)` | `onUse(state, world, pos, player, hit)` | 2 blocks + FortressAbstractBlockState mixin |
| CCA components | `readFromNbt/writeToNbt(tag)`, sync on `PacketByteBuf`, `dev.onyxstudios.cca` | `(tag, lookup)`, `RegistryByteBuf`, `org.ladysnake.cca`; entrypoint key `cardinal-components` | MineFortress + Automatone |
| Model-view stack | `MatrixStack` (`push/translate(double)/pop`) | JOML `Matrix4fStack` (`pushMatrix/translate(float)/popMatrix`), or a local `MatrixStack` seeded from it | InfoTab, EditUpgradesScreen, BlueprintsScreen, ProfessionsScreen, BlueprintRenderer |
| Food | `item.foodComponent`, `Item.isFood()` | `DataComponentTypes.FOOD` component | ServerResourceManager, EatControl |
| Item NBT | `getNbt/getSubNbt/setNbt` | `CUSTOM_DATA` / `ENTITY_DATA` components | survival inventory lock, spawn-egg mixin |
| Loot | `getLootManager().getLootTable(Identifier)` | `getReloadableRegistries().getLootTable(RegistryKey)` | fishing bobber; Automatone |
| Misc | `FabricItemSettings`, `ItemStack.canCombine`, `getScrollbarPositionX`, `BookScreen.Contents` interface, vertex `normal(Matrix3f,...)` | `Item.Settings`, `areItemsAndComponentsEqual`, `getScrollbarX`, `BookScreen.Contents` record, `normal(MatrixStack.Entry,...)` | various |
| Entity reach check mixin | redirect `Box.squaredMagnitude` in `onPlayerInteractEntity` | redirect `ServerPlayerEntity.canInteractWithEntityIn` | FortressServerPlayNetworkHandlerMixin |
| Backup prompt mixin | `start(..., canShowBackupPrompt)` arg | `@Inject` HEAD of `checkBackupAndStart` → `start(session, saveLoader, packs, onCancel)` | FortressIntegratedServerLoaderMixin (client-only: check in play-test) |
| Automatone | `EntityDimensions.width/height` fields, `getEyeHeight` invoker, `NbtHelper.toBlockPos(compound)`, `canPathfindThrough(world,pos,type)`, fake-player raw packets | record accessors, `dimensions.eyeHeight()`, `toBlockPos(nbt, key)`, `canPathfindThrough(type)`, `AutomatonePayloads` typed payloads | Automatone `mfc` |

Test harness: 1.20.6 game tests default to `skyAccess = false` (a barrier ceiling over each test); pawns spawn by world-surface height and landed on it. All tests now set `skyAccess = true`.

Known follow-ups (not regressions of this port): spawn-egg pawns are not auto-registered with a fortress (upstream stored a UUID under the block-pos key; spawn NBT no longer reaches `initialize`).

Result: build OK, no mixin-target warnings, `runServer` reaches `Done (8.8s)`, game tests 16/16 (incl. network round-trips).
