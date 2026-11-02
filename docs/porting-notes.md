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
