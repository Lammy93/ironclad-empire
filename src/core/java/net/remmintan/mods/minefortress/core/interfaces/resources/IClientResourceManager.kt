package net.remmintan.mods.minefortress.core.interfaces.resources

import net.remmintan.mods.minefortress.core.dtos.ItemInfo


interface IClientResourceManager {

    fun hasItems(stacks: List<ItemInfo>): Boolean
    fun syncRequestedItems(stacks: List<ItemInfo>)

    /** Last known contents of the fortress chests (for the Fortress storage screen); empty until the first reply. */
    val storageContents: List<ItemInfo>
    fun syncStorageContents(stacks: List<ItemInfo>)
    fun requestStorageContents()

}
