package net.remmintan.mods.minefortress.core.interfaces.entities.player;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface IFortressServerPlayerEntity {

    boolean was_InBlueprintWorldWhenLoggedOut();
    void set_WasInBlueprintWorldWhenLoggedOut(boolean wasInBlueprintWorldWhenLoggedOut);
    @Nullable
    Vec3d get_PersistedPos();

    boolean is_ModVersionValidated();

    void set_ModVersionValidated(boolean validated);

    /**
     * Where this player lands when moving to {@code destination}: their blueprint cell when entering the blueprint
     * dimension, and the remembered position when leaving it. Any other move keeps the current position.
     * (Up to 1.20.6 this was an {@code @Inject} into {@code ServerPlayerEntity.getTeleportTarget}, removed in 1.21.)
     */
    net.minecraft.world.TeleportTarget get_FortressTeleportTarget(net.minecraft.server.world.ServerWorld destination);


}
