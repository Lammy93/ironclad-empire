package net.remmintan.mods.minefortress.networking.helpers;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

/**
 * Turns away clients that do not have the mod, with a readable message, instead of letting them
 * join and fail on the first fortress packet.
 * <p>
 * Modded clients register a receiver for {@link Handshake} in the configuration phase; the server
 * checks {@code ServerConfigurationNetworking.canSend} for it before the player enters the world.
 */
public final class ModPresenceHandshake {

    public static final String REQUIRED_MESSAGE =
            "Ironclad Empire is required to join this server. Install it (with Fabric API and Fabric Language Kotlin) and try again.";

    /** Empty configuration-phase payload; only its presence matters. */
    public record Handshake() implements CustomPayload {
        public static final Id<Handshake> ID = new Id<>(Identifier.of(FortressChannelNames.NAMESPACE, "handshake"));
        public static final PacketCodec<PacketByteBuf, Handshake> CODEC = PacketCodec.unit(new Handshake());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private ModPresenceHandshake() {
    }

    /** Common initializer: payload type + the server-side check. */
    public static void registerCommon() {
        PayloadTypeRegistry.configurationS2C().register(Handshake.ID, Handshake.CODEC);
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (!ServerConfigurationNetworking.canSend(handler, Handshake.ID)) {
                LoggerFactory.getLogger(ModPresenceHandshake.class).info("Disconnecting a client without Ironclad Empire");
                handler.disconnect(Text.literal(REQUIRED_MESSAGE));
            }
        });
    }
}
