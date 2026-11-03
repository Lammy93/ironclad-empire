package org.minefortress.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.remmintan.mods.minefortress.core.utils.ServerModUtils;
import org.jetbrains.annotations.Nullable;

abstract class NamedPawnEntity extends BasePawnEntity {

    protected NamedPawnEntity(EntityType<? extends BasePawnEntity> entityType, World world, boolean enableHunger) {
        super(entityType, world, enableHunger);
    }

    @Override
    public void applySpawnData(NbtCompound entityNbt) {
        super.applySpawnData(entityNbt);
        this.setCustomNameIfNeeded();
    }

    private void setCustomNameIfNeeded() {
        ServerModUtils
                .getFortressManager(this)
                .ifPresent(it -> {
                    if (!this.hasCustomName()) {
                        final var name = it.getNameGenerator().generateRandomName();
                        this.setCustomName(Text.of(name));
                    }
                });
    }
}
