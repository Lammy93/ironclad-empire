package net.remmintan.mods.minefortress.networking.helpers;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.remmintan.mods.minefortress.core.interfaces.networking.FortressPacket;
import net.remmintan.mods.minefortress.networking.s2c.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge between MineFortress's own packet classes and Minecraft 1.20.5+ typed payloads.
 * <p>
 * Every MineFortress channel is registered as its own payload type whose body is the raw bytes the
 * packet class writes. Packet classes keep their {@code write(buf)} / {@code (buf)} constructor pair
 * and the bytes on the wire are unchanged. The buffer is a {@link RegistryByteBuf}, so packets can use
 * registry-aware codecs (item stacks) via {@code (RegistryByteBuf) buf}.
 */
public record FortressRawPayload(CustomPayload.Id<FortressRawPayload> id, byte[] data) implements CustomPayload {

    /** Server-to-client channels. Registered in common code: both sides must know every payload type. */
    static final List<String> S2C_CHANNELS = List.of(
            FortressChannelNames.FINISH_TASK,
            FortressChannelNames.FORTRESS_MANAGER_SYNC,
            FortressChannelNames.FORTRESS_SYNC_BLUEPRINT,
            FortressChannelNames.FORTRESS_REMOVE_BLUEPRINT,
            FortressChannelNames.FORTRESS_RESET_BLUEPRINT,
            FortressChannelNames.FORTRESS_BUILDINGS_SYNC,
            FortressChannelNames.FORTRESS_PROFESSION_SYNC,
            FortressChannelNames.FORTRESS_PROFESSION_INIT,
            FortressChannelNames.FORTRESS_BLUEPRINTS_PROCESS_IMPORT_EXPORT,
            S2CSyncAreasPacket.CHANNEL,
            S2CSyncFightManager.CHANNEL,
            S2CAddClientTaskPacket.CHANNEL,
            S2CSyncGamemodePacket.CHANNEL,
            S2CStartFortressConfiguration.CHANNEL,
            S2CSyncBuildingScreenInfo.CHANNEL,
            S2CSyncItemsState.CHANNEL
    );

    private static final Map<String, Id<FortressRawPayload>> IDS = new ConcurrentHashMap<>();

    @Override
    public Id<? extends CustomPayload> getId() {
        return id;
    }

    public static Id<FortressRawPayload> id(String channelName) {
        return IDS.computeIfAbsent(channelName, c -> new Id<>(Identifier.of(FortressChannelNames.NAMESPACE, c)));
    }

    private static PacketCodec<RegistryByteBuf, FortressRawPayload> codec(Id<FortressRawPayload> id) {
        return PacketCodec.of(
                (payload, buf) -> buf.writeBytes(payload.data()),
                buf -> {
                    final byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    return new FortressRawPayload(id, data);
                });
    }

    public static void registerC2S(String channelName) {
        final var id = id(channelName);
        PayloadTypeRegistry.playC2S().register(id, codec(id));
    }

    public static void registerAllS2C() {
        for (String channel : S2C_CHANNELS) {
            final var id = id(channel);
            PayloadTypeRegistry.playS2C().register(id, codec(id));
        }
    }

    /** Serialises [packet] for [channelName]. */
    public static FortressRawPayload of(String channelName, FortressPacket packet, DynamicRegistryManager registries) {
        final var buf = new RegistryByteBuf(Unpooled.buffer(), registries);
        try {
            packet.write(buf);
            final byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return new FortressRawPayload(id(channelName), data);
        } finally {
            buf.release();
        }
    }

    /** A fresh buffer over this payload's bytes, for the packet class's reading constructor. */
    public RegistryByteBuf toBuf(DynamicRegistryManager registries) {
        return new RegistryByteBuf(Unpooled.wrappedBuffer(data), registries);
    }
}
