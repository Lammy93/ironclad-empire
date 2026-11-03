package net.remmintan.mods.minefortress.networking.helpers;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.remmintan.mods.minefortress.core.interfaces.networking.FortressC2SPacket;

public class FortressClientNetworkHelper {

    public static void send(String channelName, FortressC2SPacket packet) {
        final var client = net.minecraft.client.MinecraftClient.getInstance();
        final var registries = client.getNetworkHandler() != null
                ? client.getNetworkHandler().getRegistryManager()
                : net.minecraft.registry.DynamicRegistryManager.of(net.minecraft.registry.Registries.REGISTRIES);
        ClientPlayNetworking.send(FortressRawPayload.of(channelName, packet, registries));
    }

}
