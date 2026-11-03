package org.minefortress.fortress.resources.client;

import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;

public class FortressItemStack extends ItemStack {

    public FortressItemStack(ItemConvertible item, int count) {
        super(item, count);
    }

    @Override
    public int getMaxCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public ItemStack copy() {
        if (this.isEmpty()) {
            return EMPTY;
        }
        ItemStack itemStack = new FortressItemStack(this.getItem(), this.getCount());
        itemStack.setBobbingAnimationTime(this.getBobbingAnimationTime());
        itemStack.applyChanges(this.getComponentChanges()); // 1.20.5+: components replace item NBT
        return itemStack;
    }
}
