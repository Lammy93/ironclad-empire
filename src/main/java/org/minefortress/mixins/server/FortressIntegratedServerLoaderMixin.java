package org.minefortress.mixins.server;

import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(IntegratedServerLoader.class)
public class FortressIntegratedServerLoaderMixin {

    @ModifyVariable(
            // 1.20.3+: start(Session, Dynamic, safeMode, canShowBackupPrompt, onCancel); force canShowBackupPrompt = false
            method = "start(Lnet/minecraft/world/level/storage/LevelStorage$Session;Lcom/mojang/serialization/Dynamic;ZZLjava/lang/Runnable;)V",
            at = @At("HEAD"),
            ordinal = 1,
            argsOnly = true
    )
    private boolean canShowBackupPrompt(boolean lastArg) {
        return false;
    }

}
