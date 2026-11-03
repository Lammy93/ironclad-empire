package net.remmintan.mods.minefortress.core.utils;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.Registries;
import org.minefortress.fortress.resources.client.FortressItemStack;

/**
 * Item serialisation for MineFortress packets on Minecraft 1.20.5+.
 * <p>
 * Packet buffers are {@link RegistryByteBuf}s (see FortressRawPayload). Item stacks keep the format the old
 * FortressPacketByteBufMixin used: the count is a VarInt, so stacks above 64 (fortress storage totals)
 * survive the trip, and they come back as {@link FortressItemStack}. Item NBT is replaced by data component changes.
 */
public final class PacketCompat {

    private PacketCompat() {
    }

    public static void writeItemStack(PacketByteBuf buf, ItemStack stack) {
        if (stack.isEmpty()) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        buf.writeVarInt(Registries.ITEM.getRawId(stack.getItem()));
        buf.writeVarInt(stack.getCount());
        ComponentChanges.PACKET_CODEC.encode((RegistryByteBuf) buf, stack.getComponentChanges());
    }

    public static ItemStack readItemStack(PacketByteBuf buf) {
        if (!buf.readBoolean()) {
            return ItemStack.EMPTY;
        }
        final Item item = Registries.ITEM.get(buf.readVarInt());
        final int count = buf.readVarInt();
        final ItemStack stack = count > 64 ? new FortressItemStack(item, count) : new ItemStack(item, count);
        stack.applyChanges(ComponentChanges.PACKET_CODEC.decode((RegistryByteBuf) buf));
        return stack;
    }

    public static void writeItemVariant(PacketByteBuf buf, ItemVariant variant) {
        ItemVariant.PACKET_CODEC.encode((RegistryByteBuf) buf, variant);
    }

    public static ItemVariant readItemVariant(PacketByteBuf buf) {
        return ItemVariant.PACKET_CODEC.decode((RegistryByteBuf) buf);
    }
}
