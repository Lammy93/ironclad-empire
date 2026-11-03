package net.remmintan.mods.minefortress.networking.s2c

import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.remmintan.mods.minefortress.core.dtos.ItemInfo
import net.remmintan.mods.minefortress.core.dtos.readItemInfo
import net.remmintan.mods.minefortress.core.dtos.writeItemInfo
import net.remmintan.mods.minefortress.core.interfaces.networking.FortressS2CPacket
import net.remmintan.mods.minefortress.core.utils.ClientModUtils

class S2CSyncStorageContents(private val infos: List<ItemInfo>) : FortressS2CPacket {

    constructor(buf: PacketByteBuf) : this(buf.readCollection({ mutableListOf<ItemInfo>() }) { it.readItemInfo() })

    override fun write(buf: PacketByteBuf) {
        buf.writeCollection(infos) { b, i -> b.writeItemInfo(i) }
    }

    override fun handle(client: MinecraftClient?) {
        ClientModUtils.getFortressManager().resourceManager.syncStorageContents(infos)
    }

    companion object {
        const val CHANNEL = "fortress_sync_storage_contents"
    }
}
