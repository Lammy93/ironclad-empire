package org.minefortress.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.entity.Entity
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.util.BlockRotation
import net.remmintan.mods.minefortress.core.utils.getManagersProvider
import java.util.UUID

/**
 * Upstream #26: the server crashed (null player in ServerTaskManager.tick) when the fortress owner left while
 * tasks were queued. The test player is a FakePlayer that is never in the server's player list, so to the fortress
 * its owner is always offline.
 */
class OwnerOfflineTest : FabricGameTest {

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.CREATIVE, tickLimit = 6000)
    fun tasksKeepRunningWhileOwnerOffline(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        // a queued task with nobody assigned: it waits in notStartedTasks, which the tick used to touch with a null owner
        val blueprints = f.player.getManagersProvider().get_BlueprintManager()
        val queued = blueprints.createAreaBasedTask(UUID.randomUUID(), "accessory_1", f.center.add(-10, 0, 0), BlockRotation.NONE, ctx.world)
        f.managers.taskManager.addTask(queued, emptyList(), f.player)
        // and one task handed to pawns explicitly
        val running = f.startBlueprint("accessory_1", f.center.add(8, 0, 0))
        ctx.addInstantFinalTask {
            ctx.assertTrue(running.isComplete(), "the assigned task did not finish while the owner was offline")
        }
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.CREATIVE, tickLimit = 2400)
    fun pawnInUnloadedChunkWhileOwnerOffline(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val far = f.fortressManager.professionals.first() as Entity
        // move one pawn far away: its chunk is not kept loaded, so the pawn is saved and unloaded
        far.requestTeleport(far.x + 4096, far.y, far.z)
        val running = f.startBlueprint("accessory_1", f.center.add(8, 0, 0))
        ctx.addInstantFinalTask {
            ctx.assertTrue(running.isComplete(), "the task did not finish with one pawn unloaded and the owner offline")
        }
    }
}
