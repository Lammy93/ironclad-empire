package net.remmintan.mods.minefortress.core.utils

import net.minecraft.entity.Entity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.remmintan.mods.minefortress.core.interfaces.buildings.IFortressBuilding
import net.remmintan.mods.minefortress.core.interfaces.entities.pawns.IFortressAwareEntity
import net.remmintan.mods.minefortress.core.interfaces.entities.player.IFortressPlayerEntity
import org.slf4j.LoggerFactory

/**
 * Server-side ownership checks for packets that change a fortress (issue #23). The client used to be trusted:
 * any player could destroy or hire in another fortress's buildings and command its soldiers.
 * Ownership = the target's fortress (campfire position) is the sender's fortress. Operators always pass.
 */
object FortressOwnership {

    private val log = LoggerFactory.getLogger(FortressOwnership::class.java)

    private fun fortressOf(player: ServerPlayerEntity): BlockPos? =
        (player as? IFortressPlayerEntity)?.get_FortressPos()?.orElse(null)

    private fun isOperator(player: ServerPlayerEntity) = player.hasPermissionLevel(2)

    @JvmStatic
    fun ownsEntity(player: ServerPlayerEntity, entity: Entity?, action: String): Boolean {
        val owned = isOperator(player) ||
                (entity is IFortressAwareEntity && entity.fortressPos != null && entity.fortressPos == fortressOf(player))
        if (!owned) reject(player, action, entity?.let { "entity ${it.id}" } ?: "missing entity")
        return owned
    }

    @JvmStatic
    fun ownsBuildingAt(player: ServerPlayerEntity, pos: BlockPos, action: String): Boolean {
        val building = player.world.getBlockEntity(pos) as? IFortressBuilding
        val owned = isOperator(player) ||
                (building != null && building.fortressPos != null && building.fortressPos == fortressOf(player))
        if (!owned) reject(player, action, "building at $pos")
        return owned
    }

    private fun reject(player: ServerPlayerEntity, action: String, target: String) {
        log.warn("Rejected '{}' from {}: {} is not part of their fortress", action, player.name.string, target)
    }
}
