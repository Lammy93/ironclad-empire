package org.minefortress.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.remmintan.mods.minefortress.core.FORTRESS

/**
 * Play-test bugfix-1: fortress mode reports itself as creative, so in a SURVIVAL fortress the creative item menu
 * handed out free items. The server must refuse those grabs; a CREATIVE fortress keeps the menu.
 */
class CreativeMenuTests : FabricGameTest {

    private fun grabDiamonds(f: FortressFixture): Int {
        f.player.changeGameMode(FORTRESS)
        f.player.networkHandler.onCreativeInventoryAction(CreativeInventoryActionC2SPacket(36, ItemStack(Items.DIAMOND, 64)))
        return f.player.inventory.count(Items.DIAMOND)
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun survivalFortressGetsNoCreativeItems(ctx: TestContext) {
        val got = grabDiamonds(FortressTestKit.createFortress(ctx))
        ctx.assertTrue(got == 0, "a survival fortress took $got diamonds from the creative menu")
        ctx.complete()
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.CREATIVE, tickLimit = 100)
    fun creativeFortressKeepsCreativeMenu(ctx: TestContext) {
        val got = grabDiamonds(FortressTestKit.createFortress(ctx))
        ctx.assertTrue(got == 64, "a creative fortress could not take items from the creative menu (got $got)")
        ctx.complete()
    }
}
