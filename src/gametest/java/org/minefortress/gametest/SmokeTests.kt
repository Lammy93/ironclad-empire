package org.minefortress.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.item.Items
import net.minecraft.test.AfterBatch
import net.minecraft.test.BeforeBatch
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.util.math.BlockPos
import net.minecraft.server.world.ServerWorld
import net.remmintan.mods.minefortress.blocks.FortressBlocks
import net.remmintan.mods.minefortress.core.FortressGamemode

/**
 * Smoke suite: the basic fortress loop must work at every port step.
 * Creative-batch tests run with the server's fortress mode set to CREATIVE (no resource costs);
 * survival-batch tests use the default SURVIVAL mode.
 */
class SmokeTests : FabricGameTest {

    @BeforeBatch(batchId = CREATIVE)
    fun creativeMode(world: ServerWorld) = FortressTestKit.setFortressMode(world.server, FortressGamemode.CREATIVE)

    @AfterBatch(batchId = CREATIVE)
    fun backToSurvival(world: ServerWorld) = FortressTestKit.setFortressMode(world.server, FortressGamemode.SURVIVAL)

    @GameTest(skyAccess = true, templateName = ARENA, batchId = CREATIVE, tickLimit = 400)
    fun fortressSetupCreatesCampfire(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        ctx.addInstantFinalTask {
            ctx.assertTrue(ctx.world.getBlockState(f.center).isOf(FortressBlocks.FORTRESS_CAMPFIRE), "no fortress campfire at ${f.center}")
            ctx.assertTrue(f.pawnCount() == 5, "expected 5 starting pawns, got ${f.pawnCount()}")
        }
    }

    @GameTest(skyAccess = true, templateName = ARENA, batchId = CREATIVE, tickLimit = 6000)
    fun pawnBuildsSmallBlueprint(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        // accessory_1 is the smallest predefined blueprint (14 blocks, 3x2x5)
        val origin = f.center.add(8, 0, 0)
        val task = f.startBlueprint("accessory_1", origin)
        ctx.addInstantFinalTask {
            ctx.assertTrue(task.isComplete(), "blueprint task not complete (more blocks queued: ${task.hasMoreBlocks()})")
        }
    }

    @GameTest(skyAccess = true, templateName = ARENA, batchId = SURVIVAL, tickLimit = 6000)
    fun pawnsCutTree(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        // placed after setup (setup clears trees within radius 20), at a normal working distance
        val root = f.center.add(8, 0, 6)
        FortressTestKit.placeOakTree(ctx.world, root, height = 5)
        f.cutTrees(listOf(root))
        ctx.addInstantFinalTask {
            val logs = FortressTestKit.countBlock(ctx.world, Blocks.OAK_LOG, root.add(-3, 0, -3), root.add(3, 8, 3))
            ctx.assertTrue(logs == 0, "tree still has $logs logs")
            ctx.assertTrue(f.storageCount(Items.OAK_LOG) >= 5, "expected >=5 oak logs in storage, got ${f.storageCount(Items.OAK_LOG)}")
        }
    }

    @GameTest(skyAccess = true, templateName = ARENA, batchId = CREATIVE, tickLimit = 1200)
    fun warriorKillsZombie(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val warrior = f.spawnWarrior()
        // a husk does not burn in daylight, so only the warrior can kill it
        val husk = EntityType.HUSK.spawn(ctx.world, f.center.add(5, 0, 5), SpawnReason.COMMAND)!!
        warrior.setAttackTarget(husk)
        ctx.addInstantFinalTask { ctx.assertTrue(!husk.isAlive, "husk still alive (health ${husk.health})") }
    }

    @GameTest(skyAccess = true, templateName = ARENA, batchId = SURVIVAL, tickLimit = 400)
    fun stateSurvivesSaveReload(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        f.give(Items.COBBLESTONE, 10)
        val before = f.snapshot()
        val reloaded = f.saveAndReload()
        ctx.addInstantFinalTask {
            val after = reloaded.snapshot()
            ctx.assertTrue(after == before, "state changed across save/reload: before=$before after=$after")
        }
    }

    companion object {
        const val ARENA = "minefortress:gametest/flat_64x64"
        const val CREATIVE = "minefortress_creative"
        const val SURVIVAL = "minefortress_survival"
    }
}
