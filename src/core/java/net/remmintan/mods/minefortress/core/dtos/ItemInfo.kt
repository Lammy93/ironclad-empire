@file:Suppress("UnstableApiUsage")

package net.remmintan.mods.minefortress.core.dtos

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf

data class ItemInfo(val item: ItemVariant, val amount: Long) {
    constructor(item: Item, amount: Long) : this(ItemVariant.of(item), amount)
}

fun PacketByteBuf.writeItemInfo(info: ItemInfo) {
    net.remmintan.mods.minefortress.core.utils.PacketCompat.writeItemVariant(this, info.item)
    this.writeLong(info.amount)
}

fun PacketByteBuf.readItemInfo(): ItemInfo {
    val item = net.remmintan.mods.minefortress.core.utils.PacketCompat.readItemVariant(this)
    val amount = this.readLong()

    return ItemInfo(item, amount)
}

fun ItemStack.toItemInfo(): ItemInfo {
    val item = ItemVariant.of(this)
    val amount = this.count.toLong()

    return ItemInfo(item, amount)
}

fun Item.toItemInfo(): ItemInfo {
    val item = ItemVariant.of(this)
    val amount = this.defaultStack.count.toLong()

    return ItemInfo(item, amount)
}