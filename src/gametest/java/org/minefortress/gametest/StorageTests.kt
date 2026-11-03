package org.minefortress.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.item.Items
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext

/**
 * Audit of the unreleased "items live in real chests" storage rewrite (upstream master, Jul-Aug 2025).
 * All run in SURVIVAL, where tasks reserve and consume real items.
 */
class StorageTests : FabricGameTest {

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 200)
    fun itemsLiveInRealChests(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        f.give(Items.COBBLESTONE, 64)
        ctx.addInstantFinalTask {
            ctx.assertTrue(f.chestTotal(Items.COBBLESTONE) == 64, "expected 64 cobblestone in fortress chests, found ${f.chestTotal(Items.COBBLESTONE)}")
        }
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 6000)
    fun taskReservesAndConsumes(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val origin = f.center.add(8, 0, 0)
        val cost = f.blueprintCost("accessory_1", origin)
        cost.entries.forEach { (item, n) -> f.give(item, n + 5) } // 5 spare of each, which must stay in storage
        val task = f.startBlueprint("accessory_1", origin)
        ctx.addInstantFinalTask {
            ctx.assertTrue(task.isComplete(), "blueprint task not complete")
            cost.entries.forEach { (item, _) ->
                ctx.assertTrue(f.storageCount(item) == 5, "after building, expected exactly 5 spare $item, found ${f.storageCount(item)}")
            }
        }
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 400)
    fun cancelReturnsResources(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val origin = f.center.add(8, 0, 0)
        val cost = f.blueprintCost("accessory_1", origin)
        cost.entries.forEach { (item, n) -> f.give(item, n) }
        val task = f.startBlueprint("accessory_1", origin)
        f.cancel(task)
        ctx.addInstantFinalTask {
            cost.entries.forEach { (item, n) ->
                ctx.assertTrue(f.storageCount(item) == n, "after cancel, expected $n $item back in storage, found ${f.storageCount(item)}")
            }
        }
    }

    /**
     * A fortress chest in an unloaded chunk is still the fortress's chest. Reading the storage must not load
     * (or generate) that chunk, and must not forget the chest. Loading chunks here made the server spin forever
     * while shutting down, and silently unregistered chests that were merely out of range.
     */
    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun unloadedChestStaysRegisteredAndUnloaded(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val far = f.center.add(4096, 0, 0)
        val farChunk = net.minecraft.util.math.ChunkPos(far)
        ctx.assertFalse(ctx.world.chunkManager.isChunkLoaded(farChunk.x, farChunk.z), "test setup: the far chunk is already loaded")
        f.managers.containerRegistry.register(far)

        f.storageCount(Items.COBBLESTONE) // reads the fortress storage

        ctx.assertFalse(ctx.world.chunkManager.isChunkLoaded(farChunk.x, farChunk.z), "reading storage loaded the chunk of an out-of-range chest")
        ctx.assertTrue(f.registeredContainers().contains(far), "the out-of-range chest was unregistered")
        ctx.complete()
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 400)
    fun noLossOnSaveMidTask(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val origin = f.center.add(8, 0, 0)
        val cost = f.blueprintCost("accessory_1", origin)
        cost.entries.forEach { (item, n) -> f.give(item, n) }
        val task = f.startBlueprint("accessory_1", origin)
        val reloaded = f.saveAndReload()
        reloaded.cancel(task)
        ctx.addInstantFinalTask {
            cost.entries.forEach { (item, n) ->
                val total = reloaded.storageCount(item) + reloaded.placedCount(item, task)
                ctx.assertTrue(total == n, "after save + cancel, $item in storage + placed = $total, expected $n")
            }
        }
    }
}
