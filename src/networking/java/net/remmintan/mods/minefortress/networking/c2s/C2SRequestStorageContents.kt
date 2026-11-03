package net.remmintan.mods.minefortress.networking.c2s

import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.remmintan.mods.minefortress.core.interfaces.networking.FortressC2SPacket
import net.remmintan.mods.minefortress.core.utils.ServerModUtils

/** The Fortress storage screen asks for everything in the player's own fortress chests. */
class C2SRequestStorageContents() : FortressC2SPacket {

    @Suppress("UNUSED_PARAMETER")
    constructor(buf: PacketByteBuf) : this()

    override fun handle(server: MinecraftServer, player: ServerPlayerEntity) {
        ServerModUtils.getManagersProvider(player).ifPresent { it.resourceHelper.syncStorageContents(player) }
    }

    override fun write(buf: PacketByteBuf) {}

    companion object {
        const val CHANNEL = "fortress_request_storage_contents"
    }
}
