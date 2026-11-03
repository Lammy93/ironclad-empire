package net.remmintan.mods.minefortress.networking.helpers;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.remmintan.mods.minefortress.core.interfaces.networking.FortressS2CPacket;

public class FortressServerNetworkHelper {

    public static void send(ServerPlayerEntity player, String channelName, FortressS2CPacket packet) {
        ServerPlayNetworking.send(player, FortressRawPayload.of(channelName, packet, player.getServer().getRegistryManager()));
    }

}
