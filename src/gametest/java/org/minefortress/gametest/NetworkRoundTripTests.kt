package org.minefortress.gametest

import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.RegistryByteBuf
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.remmintan.mods.minefortress.core.dtos.ItemInfo
import net.remmintan.mods.minefortress.core.interfaces.networking.FortressPacket
import net.remmintan.mods.minefortress.core.utils.PacketCompat
import net.remmintan.mods.minefortress.networking.c2s.C2SRequestItemsState
import net.remmintan.mods.minefortress.networking.helpers.FortressRawPayload
import net.remmintan.mods.minefortress.networking.s2c.S2CSyncItemsState
import org.minefortress.fortress.resources.client.FortressItemStack

/**
 * Minecraft 1.20.5+ networking: packets travel as FortressRawPayload bytes over a RegistryByteBuf.
 * Encoding a packet, decoding it with its reading constructor and encoding again must give identical bytes.
 */
class NetworkRoundTripTests : FabricGameTest {

    private fun bytesOf(packet: FortressPacket, registries: DynamicRegistryManager): ByteArray =
        FortressRawPayload.of("roundtrip_test", packet, registries).data()

    private fun roundTrip(ctx: TestContext, packet: FortressPacket, read: (PacketByteBuf) -> FortressPacket) {
        val registries = ctx.world.registryManager
        val first = bytesOf(packet, registries)
        val decoded = read(FortressRawPayload.of("roundtrip_test", packet, registries).toBuf(registries))
        val second = bytesOf(decoded, registries)
        ctx.assertTrue(first.contentEquals(second), "${packet.javaClass.simpleName} changed on a round trip (${first.size} vs ${second.size} bytes)")
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 20)
    fun itemsStateRoundTrips(ctx: TestContext) {
        roundTrip(ctx, S2CSyncItemsState(listOf(ItemInfo(Items.COBBLESTONE, 500L), ItemInfo(Items.OAK_LOG, 3L)))) { S2CSyncItemsState(it) }
        ctx.complete()
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 20)
    fun itemsRequestRoundTrips(ctx: TestContext) {
        roundTrip(ctx, C2SRequestItemsState(setOf(ItemVariant.of(Items.OAK_LOG), ItemVariant.of(Items.DIAMOND)))) { C2SRequestItemsState(it) }
        ctx.complete()
    }

    @GameTest(skyAccess = true, templateName = SmokeTests.ARENA, batchId = SmokeTests.SURVIVAL, tickLimit = 20)
    fun bigItemStackKeepsItsCount(ctx: TestContext) {
        val buf = RegistryByteBuf(Unpooled.buffer(), ctx.world.registryManager)
        PacketCompat.writeItemStack(buf, ItemStack(Items.COBBLESTONE, 200))
        val read = PacketCompat.readItemStack(buf)
        ctx.assertTrue(read.isOf(Items.COBBLESTONE), "wrong item: ${read.item}")
        ctx.assertTrue(read.count == 200, "count changed: ${read.count}")
        ctx.assertTrue(read is FortressItemStack, "stacks above 64 must come back as FortressItemStack")
        ctx.complete()
    }
}
