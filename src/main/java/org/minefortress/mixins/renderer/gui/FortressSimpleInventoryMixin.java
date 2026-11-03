package org.minefortress.mixins.renderer.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.util.collection.DefaultedList;
import net.remmintan.mods.minefortress.core.FortressGamemodeUtilsKt;
import net.remmintan.mods.minefortress.core.utils.ClientExtensionsKt;
import org.minefortress.interfaces.FortressSimpleInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SimpleInventory.class)
public abstract class FortressSimpleInventoryMixin implements FortressSimpleInventory {

    @Shadow
    @Final
    private DefaultedList<ItemStack> heldStacks;
    @Unique
    private int changeCount = 0;

    @Override
    public int getMaxCountPerStack() {
        final var clientFortressSurvival = FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER &&
                FortressGamemodeUtilsKt.isClientInFortressGamemode() &&
                ClientExtensionsKt.isSurvivalFortress(MinecraftClient.getInstance());
        if(clientFortressSurvival || FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
            return Integer.MAX_VALUE;
        else
            return FortressSimpleInventory.super.getMaxCountPerStack();
    }

    @Override
    public List<ItemStack> get_Stacks() {
        return this.heldStacks;
    }

    @Override
    public int get_OccupiedSlotWithRoomForStack(ItemStack stack) {
        for (int i = 0; i < this.heldStacks.size(); i++) {
            ItemStack itemStack = this.heldStacks.get(i);
            if(ItemStack.areItemsAndComponentsEqual(itemStack, stack))
                return i;
        }
        return -1;
    }

    @Override
    public int index_Of(ItemStack stack) {
        if(stack == null || stack.isEmpty()) return -1;
        return this.heldStacks.stream()
                .filter(it -> it.getItem() == stack.getItem())
                .findFirst()
                .map(this.heldStacks::indexOf)
                .orElse(-1);
    }

    @Override
    public void populate_RecipeFinder(RecipeMatcher recipeMatcher) {
        for (ItemStack itemStack : this.heldStacks) {
            recipeMatcher.addInput(itemStack, Integer.MAX_VALUE);
        }
    }

    @Override
    public int get_ChangeCount() {
        return this.changeCount;
    }

    @Inject(method = "markDirty", at = @At("RETURN"))
    public void markDirty(CallbackInfo ci) {
        changeCount++;
    }

}
