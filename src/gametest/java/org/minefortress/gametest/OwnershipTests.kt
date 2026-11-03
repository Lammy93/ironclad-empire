package org.minefortress.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameMode
import net.remmintan.mods.minefortress.blocks.FortressBlocks
import net.remmintan.mods.minefortress.core.interfaces.buildings.IFortressBuilding
import net.remmintan.mods.minefortress.networking.c2s.C2SDestroyBuilding
import net.remmintan.mods.minefortress.networking.c2s.C2SFollowTargetPacket
import net.remmintan.mods.minefortress.networking.c2s.C2SMoveTargetPacket

/**
 * Issue #23: the server trusted the client about ownership. Player B must not be able to change player A's
 * fortress: destroy its buildings, command its soldiers, or mine its campfire (which deletes the whole fortress).
 */
class OwnershipTests : FabricGameTest {

    private fun twoFortresses(ctx: TestContext) =
        FortressTestKit.createFortress(ctx, BlockPos(12, 1, 12)) to FortressTestKit.createFortress(ctx, BlockPos(52, 1, 52))

    private fun firstBuildingPos(ctx: TestContext, f: FortressFixture): BlockPos =
        BlockPos.iterate(f.center.add(-12, -3, -12), f.center.add(12, 6, 12))
            .firstOrNull { ctx.world.getBlockEntity(it) is IFortressBuilding }?.toImmutable()
            ?: error("fortress has no building block entity")

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun cannotDestroyOthersBuilding(ctx: TestContext) {
        val (a, b) = twoFortresses(ctx)
        val pos = firstBuildingPos(ctx, a)
        C2SDestroyBuilding(pos).handle(ctx.world.server, b.player)
        ctx.addInstantFinalTask {
            ctx.assertTrue(ctx.world.getBlockEntity(pos) is IFortressBuilding, "player B destroyed player A's building at $pos")
        }
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun cannotCommandOthersSoldier(ctx: TestContext) {
        val (a, b) = twoFortresses(ctx)
        val warrior = a.spawnWarrior()
        val warriorId = (warrior as Entity).id
        val bait = EntityType.PIG.spawn(ctx.world, a.center.add(4, 0, 4), SpawnReason.COMMAND)!!
        C2SMoveTargetPacket(b.center, warriorId).handle(ctx.world.server, b.player)
        C2SFollowTargetPacket(warriorId, bait.id).handle(ctx.world.server, b.player)
        ctx.assertTrue(warrior.moveTarget == null, "player B moved player A's warrior to ${warrior.moveTarget}")
        ctx.assertTrue(warrior.attackTarget == null, "player B set player A's warrior to attack ${warrior.attackTarget}")
        ctx.complete()
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun campfireOnlyBreakableByOwner(ctx: TestContext) {
        val (a, b) = twoFortresses(ctx)
        b.player.changeGameMode(GameMode.SURVIVAL)
        b.player.interactionManager.tryBreakBlock(a.center)
        ctx.assertTrue(ctx.world.getBlockState(a.center).isOf(FortressBlocks.FORTRESS_CAMPFIRE), "player B mined player A's fortress campfire")
        ctx.complete()
    }

    /** The campfire holds the whole fortress; not even its owner may mine it in survival (only operators). */
    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun ownerCannotMineOwnCampfireEither(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        f.player.changeGameMode(GameMode.SURVIVAL)
        f.player.interactionManager.tryBreakBlock(f.center)
        ctx.assertTrue(ctx.world.getBlockState(f.center).isOf(FortressBlocks.FORTRESS_CAMPFIRE), "the owner mined their own fortress campfire")
        ctx.complete()
    }

    /** If a campfire disappears anyway (operator /setblock), the fortress's buildings must not crash the server. */
    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 200)
    fun missingCampfireDoesNotCrashTheServer(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        ctx.world.setBlockState(f.center, net.minecraft.block.Blocks.AIR.defaultState)
        ctx.runAtTick(100) { ctx.complete() } // the crash happened within a few ticks of the campfire vanishing
    }
}
