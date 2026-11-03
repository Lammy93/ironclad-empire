package org.minefortress.gametest

import com.mojang.authlib.GameProfile
import net.fabricmc.fabric.api.entity.FakePlayer
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.LeavesBlock
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.test.TestContext
import net.minecraft.util.BlockRotation
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.remmintan.mods.minefortress.core.FortressGamemode
import net.remmintan.mods.minefortress.core.TaskType
import net.remmintan.mods.minefortress.core.interfaces.IFortressGamemodeHolder
import net.remmintan.mods.minefortress.core.interfaces.entities.pawns.ITargetedPawn
import net.remmintan.mods.minefortress.core.interfaces.entities.player.IFortressPlayerEntity
import net.remmintan.mods.minefortress.core.interfaces.selections.ServerSelectionType
import net.remmintan.mods.minefortress.core.interfaces.server.IServerFortressManager
import net.remmintan.mods.minefortress.core.interfaces.server.IServerManagersProvider
import net.remmintan.mods.minefortress.core.utils.getFortressManager
import net.remmintan.mods.minefortress.core.utils.getManagersProvider
import org.minefortress.entity.BasePawnEntity
import org.minefortress.registries.FortressEntities
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Drives MineFortress from server code for headless game tests.
 *
 * The test "player" is a Fabric [FakePlayer]. It is never ticked and never appears in the server's
 * player list, so the kit does by hand what a connected client would trigger: it ticks the player's
 * blueprint manager once and hands tasks to explicit pawns (the task manager only auto-assigns
 * queued tasks while the fortress owner is online).
 */
object FortressTestKit {

    private val playerCounter = AtomicInteger()

    fun setFortressMode(server: MinecraftServer, mode: FortressGamemode) {
        (server.saveProperties as IFortressGamemodeHolder).set_fortressGamemode(mode)
    }

    /** Creates a fortress with its campfire at [center] (relative to the test structure). */
    fun createFortress(ctx: TestContext, center: BlockPos = BlockPos(16, 1, 16)): FortressFixture {
        val world = ctx.world
        val server = world.server
        val n = playerCounter.incrementAndGet()
        val name = "mfc_test_$n"
        val player = FakePlayer.get(world, GameProfile(UUID.nameUUIDFromBytes(name.toByteArray()), name))
        val start = ctx.getAbsolutePos(center)
        player.refreshPositionAndAngles(start.x + 0.5, start.y + 2.0, start.z + 0.5, 0f, 0f)

        val blueprints = player.getManagersProvider().get_BlueprintManager()
        blueprints.tick(server, world, player) // loads blueprints; normally done by ServerPlayerEntity.tick
        val fortressPos = blueprints.createInstantPlaceTask("campfire", start, BlockRotation.NONE).execute(world, player)
        (player as IFortressPlayerEntity).set_FortressPos(fortressPos)

        val fixture = FortressFixture(ctx, player, fortressPos)
        fixture.fortressManager.setSpawnPawns(false) // no random extra pawns: tests need stable counts
        return fixture
    }

    fun countNonAir(world: ServerWorld, from: BlockPos, to: BlockPos): Int =
        BlockPos.iterate(from, to).count { !world.getBlockState(it).isAir }

    fun countBlock(world: ServerWorld, block: Block, from: BlockPos, to: BlockPos): Int =
        BlockPos.iterate(from, to).count { world.getBlockState(it).isOf(block) }

    /** A plain oak tree: [height] logs and a leaf crown that will not decay while the logs stand. */
    fun placeOakTree(world: ServerWorld, root: BlockPos, height: Int) {
        val leaves = Blocks.OAK_LEAVES.defaultState.with(LeavesBlock.DISTANCE, 1).with(LeavesBlock.PERSISTENT, false)
        val top = root.up(height - 1)
        for (dy in -1..1) for (dx in -2..2) for (dz in -2..2) {
            val radius = if (dy == 1) 1 else 2
            if (Math.abs(dx) > radius || Math.abs(dz) > radius) continue
            world.setBlockState(top.add(dx, dy, dz), leaves)
        }
        for (i in 0 until height) world.setBlockState(root.up(i), Blocks.OAK_LOG.defaultState)
    }
}

data class FortressSnapshot(val pawns: Int, val cobblestone: Int, val professions: Map<String, Int>)

class FortressFixture(
    private val ctx: TestContext,
    val player: ServerPlayerEntity,
    /** Absolute position of the fortress campfire (the fortress id). */
    val center: BlockPos,
) {
    private val world: ServerWorld get() = ctx.world
    val fortressManager: IServerFortressManager get() = world.server.getFortressManager(center)!!
    val managers: IServerManagersProvider get() = world.server.getManagersProvider(center)

    fun pawnCount(): Int = fortressManager.totalColonistsCount

    fun pawnIds(): Set<Int> = fortressManager.professionals.map { (it as Entity).id }.toSet()

    fun readyWorkerIds(): Set<Int> = fortressManager.readyWorkers.map { (it as Entity).id }.toSet()

    fun give(item: Item, count: Int) {
        check(managers.resourceHelper.putItemsToSuitableContainer(listOf(ItemStack(item, count)))) {
            "fortress storage refused $count x $item"
        }
    }

    fun storageCount(item: Item): Int = managers.resourceHelper.getCountIncludingSimilar(item).toInt()

    /** Chest positions registered as this fortress's storage (read from the manager's saved form). */
    fun registeredContainers(): Set<BlockPos> {
        val tag = NbtCompound()
        (managers.resourceManager as net.remmintan.mods.minefortress.core.interfaces.server.IWritableManager).write(tag)
        return tag.getLongArray("containerPositions").map { BlockPos.fromLong(it) }.toSet()
    }

    /** Counts [item] physically sitting in chests within 16 blocks of the campfire. */
    fun chestTotal(item: Item): Int =
        BlockPos.iterate(center.add(-16, -4, -16), center.add(16, 8, 16))
            .mapNotNull { world.getBlockEntity(it) as? net.minecraft.block.entity.ChestBlockEntity }
            .sumOf { chest -> (0 until chest.size()).sumOf { slot -> chest.getStack(slot).let { if (it.isOf(item)) it.count else 0 } } }

    /** Items a blueprint build at [origin] needs, merged per item. */
    fun blueprintCost(blueprintId: String, origin: BlockPos): Map<Item, Int> {
        val blueprints = player.getManagersProvider().get_BlueprintManager()
        val task = blueprints.createAreaBasedTask(UUID.randomUUID(), blueprintId, origin, BlockRotation.NONE, world)
            as org.minefortress.tasks.AreaBlueprintTask
        return task.requiredResources.groupBy { it.item }.mapValues { (_, stacks) -> stacks.sumOf { it.count } }
    }

    fun cancel(task: net.remmintan.mods.minefortress.core.interfaces.tasks.IBaseTask) =
        managers.taskManager.cancelTask(task.pos, player)

    /** How many of the task's positions already hold the block form of [item]. */
    fun placedCount(item: Item, task: net.remmintan.mods.minefortress.core.interfaces.tasks.IBaseTask): Int =
        task.positions.count { world.getBlockState(it).block.asItem() == item }

    fun professionAmounts(): Map<String, Int> =
        PROFESSION_IDS.associateWith { managers.professionsManager.getProfession(it)?.amount ?: 0 }

    fun snapshot() = FortressSnapshot(pawnCount(), storageCount(net.minecraft.item.Items.COBBLESTONE), professionAmounts())

    fun startBlueprint(blueprintId: String, origin: BlockPos): net.remmintan.mods.minefortress.core.interfaces.tasks.IAreaBasedTask {
        val blueprints = player.getManagersProvider().get_BlueprintManager()
        val task = blueprints.createAreaBasedTask(UUID.randomUUID(), blueprintId, origin, BlockRotation.NONE, world)
        assign(task)
        return task
    }

    fun cutTrees(roots: List<BlockPos>) {
        val task = managers.tasksCreator.createCutTreesTask(roots)
        assign(task)
    }

    fun dig(from: BlockPos, to: BlockPos) {
        val hit = BlockHitResult(Vec3d.ofCenter(from), Direction.UP, from, false)
        val positions = BlockPos.iterate(from, to).map { it.toImmutable() }
        val task = managers.tasksCreator.createSelectionTask(TaskType.REMOVE, from, to, ServerSelectionType.SQUARES, hit, positions, player)
        assign(task)
    }

    /** Hands [task] to every ready worker; fails loudly if nobody can take it. */
    private fun assign(task: net.remmintan.mods.minefortress.core.interfaces.tasks.IBaseTask) {
        val ready = readyWorkerIds()
        check(ready.isNotEmpty()) { "no ready workers to take the task (pawns=${pawnCount()}, professionals=${fortressManager.professionals.size})" }
        managers.taskManager.addTask(task, ready.toList(), player)
        check(readyWorkerIds().size < ready.size) { "task was not taken by any of ${ready.size} ready workers" }
    }

    /** Spawns a warrior pawn that belongs to this fortress, next to the campfire. */
    fun spawnWarrior(): ITargetedPawn {
        val tag = NbtCompound().apply { putLong(BasePawnEntity.FORTRESS_CENTER_BLOCK_KEY, center.asLong()) }
        val pos = center.add(2, 0, 2)
        val warrior = FortressEntities.WARRIOR_PAWN_ENTITY_TYPE.spawn(world, { BasePawnEntity.applySpawnDataTo(it, tag) }, pos, SpawnReason.MOB_SUMMONED, true, false)
            ?: error("warrior did not spawn")
        return warrior as ITargetedPawn
    }

    /**
     * Simulates saving and loading the fortress: the campfire block entity (which owns every manager)
     * is serialised, removed and recreated from that NBT; the live pawns then re-register the way
     * they do when their chunk loads.
     */
    fun saveAndReload(): FortressFixture {
        val pawns = fortressManager.professionals.map { it as LivingEntity }
        val old = world.getBlockEntity(center) ?: error("no campfire block entity at $center")
        val nbt = old.createNbtWithId(world.registryManager)
        val state = world.getBlockState(center)
        world.removeBlockEntity(center)
        val fresh = BlockEntity.createFromNbt(center, state, nbt, world.registryManager) ?: error("campfire block entity could not be recreated")
        world.addBlockEntity(fresh)
        val reloaded = FortressFixture(ctx, player, center)
        pawns.forEach { reloaded.fortressManager.addPawn(it) }
        return reloaded
    }

    companion object {
        val PROFESSION_IDS = listOf("colonist", "miner1", "lumberjack1", "forester", "farmer", "crafter", "blacksmith", "warrior1", "archer1")
    }
}
