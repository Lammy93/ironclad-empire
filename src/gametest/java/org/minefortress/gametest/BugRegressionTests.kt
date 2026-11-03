package org.minefortress.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.util.math.BlockPos
import net.remmintan.mods.minefortress.blocks.FortressBlocks

/** Regression tests for open upstream bug reports (remmintan/minefortress). */
class BugRegressionTests : FabricGameTest {

    /** Upstream #48: pawns could dig up the fortress campfire, which soft-locks the fortress. */
    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.CREATIVE, tickLimit = 3000)
    fun digTaskSkipsCampfire(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val from = f.center.add(-2, -1, -2)
        val to = f.center.add(2, 0, 2)
        f.dig(from, to)
        ctx.addInstantFinalTask {
            ctx.assertTrue(ctx.world.getBlockState(f.center).isOf(FortressBlocks.FORTRESS_CAMPFIRE), "the fortress campfire was dug up")
            val others = BlockPos.iterate(from, to).count { it != f.center && !ctx.world.getBlockState(it).isAir }
            ctx.assertTrue(others == 0, "dig task not finished yet ($others blocks left); ${f.diagnostics()}")
        }
    }

    /**
     * Upstream #29: soldiers did not fight back when attacked by something that is not a hostile mob (another player,
     * another fortress's pawns). An angry wolf stands in for that attacker; the soldier must hurt it back.
     */
    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.CREATIVE, tickLimit = 600)
    fun idleWarriorFightsBackNonHostileAttacker(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val warrior = f.spawnWarrior() as LivingEntity
        val wolf = angryWolfAt(ctx, warrior)
        ctx.addInstantFinalTask { ctx.assertTrue(wolf.health < wolf.maxHealth || !wolf.isAlive, "idle warrior never hit back at its attacker") }
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.CREATIVE, tickLimit = 600)
    fun idleArcherFightsBackNonHostileAttacker(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val archer = f.spawnArcher() as LivingEntity
        val wolf = angryWolfAt(ctx, archer)
        ctx.addInstantFinalTask { ctx.assertTrue(wolf.health < wolf.maxHealth || !wolf.isAlive, "idle archer never hit back at its attacker") }
    }

    private fun angryWolfAt(ctx: TestContext, victim: LivingEntity): net.minecraft.entity.passive.WolfEntity {
        val wolf = EntityType.WOLF.spawn(ctx.world, victim.blockPos.add(3, 0, 0), SpawnReason.COMMAND)!!
        wolf.setAngryAt(victim.uuid)
        wolf.target = victim
        return wolf
    }
}
