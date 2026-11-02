# Storage rewrite audit (Task 2)

Upstream `master` has 17 commits after the 1.17.5-beta release (2025-07-05 → 2025-08-10).
They replace the old virtual "fortress inventory" with items kept in real chests in the world,
using the Fabric Transfer API. None of it was ever released. This audit decides whether we keep it.

## Automated result (Minecraft 1.20.2, 2026-11-02)

`scripts/gradle.sh runGametest` → **11/11 passed**, including:

| Test | What it proves | Result |
|---|---|---|
| `StorageTests.itemsLiveInRealChests` | Items given to the fortress are physically in its chests | pass |
| `StorageTests.taskReservesAndConsumes` | A blueprint build uses exactly its cost; spare items stay in storage | pass |
| `StorageTests.cancelReturnsResources` | Cancelling a build returns every reserved item | pass |
| `StorageTests.noLossOnSaveMidTask` | Saving the fortress mid-build and then cancelling loses nothing | pass |
| `TwoFortressesIsolationTest.storageIsPerFortress` | Two fortresses never share storage | pass |

These are characterisation tests of existing code: passing on the first run is the "accept" outcome.

## Manual play-test

Drop: `C:\Users\ozzyz\Documents\MineFortress Continued\MFC-storage-1.20.2` (checklist in `WHAT-TO-TRY.txt`).
Result: _pending Zac's play-test_.

## Decision

Pending the play-test. If the play-test is OK, the rewrite is accepted as the storage model for Phase 1.

## Storage API (what later tasks build on)

All on `IServerManagersProvider` (`server.getManagersProvider(fortressPos)`):

| Need | Call |
|---|---|
| Add items (all-or-nothing) | `resourceHelper.putItemsToSuitableContainer(List<ItemStack>): Boolean` |
| Count an item | `resourceHelper.getCountIncludingSimilar(Item): Long` |
| Check a list is available | `resourceManager.hasItems(List<ItemInfo>)` |
| Link / unlink a chest | `containerRegistry.register(pos)` / `unregister(pos)` |
| Combined storage view | `ServerResourceManager.getStorage(): Storage<ItemVariant>` (Fabric Transfer API) |
| Task reservations | Items move into the task's `FortressTaskBlockEntity` (16-slot inventory at the task position); `ServerTaskManager.cancelTask` → `resourceHelper.transferItemsFromTask` returns them |

Known gaps found while auditing (not storage bugs, recorded for later tasks):

- `ServerTaskManager.write/read` are empty: queued and in-progress tasks are not saved with the world.
  Reserved items survive (they sit in the task block), but the task itself is gone after a reload.
- `PlaceControl.kt:100` only pays items out of the task inventory in *creative* fortresses, which looks inverted.
  Blueprint builds are unaffected (they pay through `AreaBlueprintTask.removeReservedItem`).
