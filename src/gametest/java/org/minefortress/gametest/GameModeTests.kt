package org.minefortress.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.nbt.NbtCompound
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.world.GameMode
import net.remmintan.mods.minefortress.core.FORTRESS
import net.remmintan.mods.minefortress.core.isFortressGamemode

/** The fortress game mode (injected into vanilla's GameMode enum via Fabric-ASM) must keep working. */
class GameModeTests : FabricGameTest {

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun fortressModeRoundTrip(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        f.player.changeGameMode(FORTRESS)
        ctx.assertTrue(isFortressGamemode(f.player), "player should be in fortress mode")
        f.player.changeGameMode(GameMode.SURVIVAL)
        ctx.assertFalse(isFortressGamemode(f.player), "player should have left fortress mode")
        ctx.complete()
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun fortressModePersists(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        f.player.changeGameMode(FORTRESS)
        val saved = f.player.writeNbt(NbtCompound())
        val id = saved.getInt("playerGameType")
        ctx.assertTrue(GameMode.byId(id) == FORTRESS, "saved game mode id $id does not load back as FORTRESS")
        ctx.complete()
    }
}
