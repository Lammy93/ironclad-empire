package org.minefortress.mixins.renderer.gui;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.5+ blurs the frame in Screen.renderBackground. Our screens were written for 1.20.2 and call
 * renderBackground themselves (often again through super.render), so the second blur smeared their own text.
 * Our screens keep the dark backdrop but skip the blur, as they looked before 1.20.5.
 */
@Mixin(Screen.class)
public abstract class FortressScreenBlurMixin {

    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void minefortress$noBlurOnFortressScreens(float delta, CallbackInfo ci) {
        final String name = this.getClass().getName();
        if (name.startsWith("org.minefortress.") || name.startsWith("net.remmintan.mods.minefortress.")) {
            ci.cancel();
        }
    }
}
