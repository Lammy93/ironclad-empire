package org.minefortress.mixins.server;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Never show the "make a backup?" prompt when opening a world (MineFortress worlds carry custom
 * dimensions, which would otherwise trigger it every time).
 * <p>
 * Up to 1.20.4 this forced the {@code canShowBackupPrompt} argument of {@code start(...)} to false.
 * Since 1.20.5 the prompt is shown by {@code checkBackupAndStart}, so skip straight to {@code start}.
 */
@Mixin(IntegratedServerLoader.class)
public abstract class FortressIntegratedServerLoaderMixin {

    @Shadow
    protected abstract void start(LevelStorage.Session session, SaveLoader saveLoader, ResourcePackManager dataPackManager, Runnable onCancel);

    @Inject(method = "checkBackupAndStart", at = @At("HEAD"), cancellable = true)
    private void skipBackupPrompt(LevelStorage.Session session, SaveLoader saveLoader, ResourcePackManager dataPackManager, Runnable onCancel, CallbackInfo ci) {
        this.start(session, saveLoader, dataPackManager, onCancel);
        ci.cancel();
    }
}
