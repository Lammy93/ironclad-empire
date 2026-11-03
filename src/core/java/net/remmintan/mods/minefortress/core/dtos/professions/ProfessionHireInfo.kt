package net.remmintan.mods.minefortress.core.dtos.professions

import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf

data class ProfessionHireInfo(
    val professionId: String,
    val professionName: String,
    val professionItem: ItemStack,
    val professionCost: List<ProfessionCost>
) {
    fun writeToPacketByteBuf(buf: PacketByteBuf) {
        buf.writeString(professionId)
        buf.writeString(professionName)
        net.remmintan.mods.minefortress.core.utils.PacketCompat.writeItemStack(buf, professionItem)
        buf.writeCollection(professionCost) { b, it -> it.writeToPacketByteBuf(b) }
    }

    companion object {
        fun readFromPacketByteBuf(buf: PacketByteBuf): ProfessionHireInfo {
            val professionId = buf.readString()
            val professionName = buf.readString()
            val professionItem = net.remmintan.mods.minefortress.core.utils.PacketCompat.readItemStack(buf)
            val professionCosts =
                buf.readCollection({ mutableListOf<ProfessionCost>() }) { ProfessionCost.readFromPacketByteBuf(it) }
                    .toList()

            return ProfessionHireInfo(professionId, professionName, professionItem, professionCosts)
        }
    }
}
