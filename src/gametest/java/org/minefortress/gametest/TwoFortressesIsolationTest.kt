package org.minefortress.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.entity.Entity
import net.minecraft.item.Items
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.util.math.BlockPos

/** Two players with separate fortresses on one server never affect each other (plan Review Focus 2). */
class TwoFortressesIsolationTest : FabricGameTest {

    @GameTest(templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 400)
    fun storageIsPerFortress(ctx: TestContext) {
        val a = FortressTestKit.createFortress(ctx, BlockPos(12, 1, 12))
        val b = FortressTestKit.createFortress(ctx, BlockPos(52, 1, 52))
        a.give(Items.COBBLESTONE, 10)
        ctx.addInstantFinalTask {
            ctx.assertTrue(a.storageCount(Items.COBBLESTONE) == 10, "A should have 10 cobblestone, has ${a.storageCount(Items.COBBLESTONE)}")
            ctx.assertTrue(b.storageCount(Items.COBBLESTONE) == 0, "B should have 0 cobblestone, has ${b.storageCount(Items.COBBLESTONE)}")
        }
    }

    @GameTest(templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 400)
    fun taskOfOneFortressNeverTakesTheOthersPawns(ctx: TestContext) {
        val a = FortressTestKit.createFortress(ctx, BlockPos(12, 1, 12))
        val b = FortressTestKit.createFortress(ctx, BlockPos(52, 1, 52))
        val bPawnIds = b.pawnIds()
        val aPawnIds = a.pawnIds()
        ctx.assertTrue(aPawnIds.intersect(bPawnIds).isEmpty(), "fortresses share pawns: ${aPawnIds.intersect(bPawnIds)}")
        val bReadyBefore = b.readyWorkerIds()
        a.dig(a.center.add(4, -1, 4), a.center.add(5, -1, 5)) // the stone floor next to A
        ctx.addInstantFinalTask {
            ctx.assertTrue(b.readyWorkerIds() == bReadyBefore, "B's ready workers changed after A's task: $bReadyBefore -> ${b.readyWorkerIds()}")
        }
    }
}
