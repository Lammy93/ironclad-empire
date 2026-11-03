package org.minefortress.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.remmintan.mods.minefortress.networking.c2s.C2SSwitchToFortressModePacket
import net.remmintan.mods.minefortress.networking.c2s.C2SSwitchToMinecraftSurvival

/**
 * Upstream #49: going back to survival from fortress mode ("Explore your village!") deleted the items the player
 * carried before entering fortress mode. They must come back exactly, also after the player is saved and loaded.
 */
class ModeSwitchInventoryTest : FabricGameTest {

    private fun inventorySnapshot(f: FortressFixture): List<String> {
        val inv = f.player.inventory
        return (0 until inv.size()).map { i -> inv.getStack(i).let { if (it.isEmpty) "-" else "${Registries.ITEM.getId(it.item)}x${it.count}" } }
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun survivalItemsComeBack(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        f.player.inventory.insertStack(ItemStack(Items.DIAMOND, 32))
        f.player.inventory.insertStack(ItemStack(Items.IRON_SWORD))
        val before = inventorySnapshot(f)

        C2SSwitchToFortressModePacket().handle(ctx.world.server, f.player)
        C2SSwitchToMinecraftSurvival().handle(ctx.world.server, f.player)

        ctx.assertTrue(inventorySnapshot(f) == before, "survival inventory changed: before=${before.filter { it != "-" }} after=${inventorySnapshot(f).filter { it != "-" }}")
        ctx.complete()
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun fullInventoryRoundTrip(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        val items = Registries.ITEM.stream().filter { it != Items.AIR }.limit(64).toList()
        val inv = f.player.inventory
        for (slot in 0 until inv.size()) inv.setStack(slot, ItemStack(items[slot % items.size], 1 + slot % 7))
        val before = inventorySnapshot(f)

        C2SSwitchToFortressModePacket().handle(ctx.world.server, f.player)
        C2SSwitchToMinecraftSurvival().handle(ctx.world.server, f.player)

        ctx.assertTrue(inventorySnapshot(f) == before, "a full inventory (incl. armour and off-hand) did not come back slot for slot")
        ctx.complete()
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 100)
    fun stashSurvivesPlayerSaveAndLoad(ctx: TestContext) {
        val f = FortressTestKit.createFortress(ctx)
        f.player.inventory.insertStack(ItemStack(Items.EMERALD, 17))
        val before = inventorySnapshot(f)

        C2SSwitchToFortressModePacket().handle(ctx.world.server, f.player)
        // the player logs off (or the game crashes) while in fortress mode, and loads back in
        val saved = f.player.writeNbt(NbtCompound())
        f.player.inventory.clear()
        f.player.readNbt(saved)
        C2SSwitchToMinecraftSurvival().handle(ctx.world.server, f.player)

        ctx.assertTrue(inventorySnapshot(f) == before, "set-aside survival items were lost across a player save/load")
        ctx.complete()
    }
}
