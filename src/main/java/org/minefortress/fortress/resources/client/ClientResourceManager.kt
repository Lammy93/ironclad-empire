package org.minefortress.fortress.resources.client

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.remmintan.mods.minefortress.core.dtos.EnoughResourceState
import net.remmintan.mods.minefortress.core.dtos.ItemInfo
import net.remmintan.mods.minefortress.core.interfaces.resources.IClientResourceManager
import net.remmintan.mods.minefortress.networking.c2s.C2SRequestItemsState
import net.remmintan.mods.minefortress.networking.c2s.C2SRequestStorageContents
import net.remmintan.mods.minefortress.networking.helpers.FortressClientNetworkHelper
import java.time.Duration
import java.time.Instant

@Suppress("UnstableApiUsage")
class ClientResourceManager : IClientResourceManager {

    private val items = mutableMapOf<ItemVariant, ItemStateInfo>()

    private val pendingRequests = mutableSetOf<ItemVariant>()

    override fun hasItems(stacks: List<ItemInfo>): Boolean {
        val result = stacks.map { st ->
            when (val storageStack = getStorageStack(st)) {
                is ItemStateInfo.RequestedItemState -> EnoughResourceState.PENDING
                is ItemStateInfo.FilledItemState -> if (storageStack.amount >= st.amount) EnoughResourceState.ENOUGH else EnoughResourceState.NOT_ENOUGH
            }
        }.all { it == EnoughResourceState.ENOUGH }

        requestAllPending()

        return result
    }

    override fun syncRequestedItems(stacks: List<ItemInfo>) {
        stacks.forEach {
            items[it.item] = ItemStateInfo.FilledItemState(it.amount)
        }
    }

    override var storageContents: List<ItemInfo> = emptyList()
        private set

    override fun syncStorageContents(stacks: List<ItemInfo>) {
        storageContents = stacks
        stacks.forEach { items[it.item] = ItemStateInfo.FilledItemState(it.amount) }
    }

    override fun requestStorageContents() {
        FortressClientNetworkHelper.send(C2SRequestStorageContents.CHANNEL, C2SRequestStorageContents())
    }

    private fun getStorageStack(requestedStack: ItemInfo): ItemStateInfo {
        val item = requestedStack.item
        val now = Instant.now()
        val itemStateInfo = items.computeIfAbsent(item) { scheduleRequest(requestedStack) }
        if (itemStateInfo is ItemStateInfo.RequestedItemState && itemStateInfo.shouldRequestAgain(now)) {
            items[item] = scheduleRequest(requestedStack)
        }
        if (itemStateInfo is ItemStateInfo.FilledItemState) {
            if (itemStateInfo.expired(now)) {
                val requested = scheduleRequest(requestedStack)
                items[item] = requested
                return requested
            }
            if (itemStateInfo.shouldRefresh(now)) {
                items[item] = scheduleRequest(requestedStack)
            }
        }
        return itemStateInfo
    }

    private fun scheduleRequest(stack: ItemInfo): ItemStateInfo.RequestedItemState {
        pendingRequests.add(stack.item)
        return ItemStateInfo.RequestedItemState()
    }

    private fun requestAllPending() {
        val packet = C2SRequestItemsState(pendingRequests)
        FortressClientNetworkHelper.send(C2SRequestItemsState.CHANNEL, packet)
        pendingRequests.clear()
    }

}


private sealed interface ItemStateInfo {
    data class FilledItemState(val amount: Long, private val cachedAt: Instant = Instant.now()) :
        ItemStateInfo {

        fun shouldRefresh(currentTime: Instant): Boolean {
            return Duration.between(cachedAt, currentTime).toSeconds() > 1
        }

        fun expired(currentTime: Instant): Boolean {
            return Duration.between(cachedAt, currentTime).toSeconds() > 30
        }

    }

    data class RequestedItemState(private val requestedAt: Instant = Instant.now()) : ItemStateInfo {
        fun shouldRequestAgain(currentTime: Instant): Boolean {
            return Duration.between(requestedAt, currentTime).toSeconds() > 5
        }
    }
}

