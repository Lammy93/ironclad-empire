package org.minefortress.mixins;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Lets several actions share one key (vanilla only triggers the last binding registered for a key).
 * Fortress controls deliberately reuse keys of vanilla actions. Up to 1.20 the original mod required the
 * separate "Keybind Fix" mod for this; it has no 1.21 build, so the behaviour lives here now.
 */
@Mixin(KeyBinding.class)
public abstract class FortressKeyBindingMixin {

    @Shadow @Final private static Map<String, KeyBinding> KEYS_BY_ID;
    @Shadow private InputUtil.Key boundKey;
    @Shadow private int timesPressed;

    @Shadow public abstract void setPressed(boolean pressed);

    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true)
    private static void minefortress$pressEveryBinding(InputUtil.Key key, CallbackInfo ci) {
        for (KeyBinding binding : KEYS_BY_ID.values()) {
            final FortressKeyBindingMixin self = (FortressKeyBindingMixin) (Object) binding;
            if (key.equals(self.boundKey)) {
                self.timesPressed++;
            }
        }
        ci.cancel();
    }

    @Inject(method = "setKeyPressed", at = @At("HEAD"), cancellable = true)
    private static void minefortress$setEveryBinding(InputUtil.Key key, boolean pressed, CallbackInfo ci) {
        for (KeyBinding binding : KEYS_BY_ID.values()) {
            final FortressKeyBindingMixin self = (FortressKeyBindingMixin) (Object) binding;
            if (key.equals(self.boundKey)) {
                self.setPressed(pressed);
            }
        }
        ci.cancel();
    }
}
