package org.minefortress;


import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.remmintan.mods.minefortress.blocks.FortressBlocks;
import net.remmintan.mods.minefortress.core.dtos.PawnSkin;
import net.remmintan.mods.minefortress.core.dtos.SupportLevel;
import net.remmintan.mods.minefortress.core.interfaces.server.IServerFortressManager;
import net.remmintan.mods.minefortress.core.interfaces.server.IServerManagersProvider;
import net.remmintan.mods.minefortress.core.services.FortressManagerLocator;
import net.remmintan.mods.minefortress.gui.FortressHandledScreensKt;
import net.remmintan.mods.minefortress.networking.registries.ServerNetworkReceivers;
import org.minefortress.commands.CommandsManager;
import org.minefortress.fortress.ServerFortressManager;
import org.minefortress.fortress.ServerManagersProvider;
import org.minefortress.registries.FortressEntities;
import org.minefortress.registries.FortressItems;
import org.minefortress.registries.events.FortressServerEvents;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class MineFortressMod implements ModInitializer {

    public static final String BLUEPRINTS_FOLDER_NAME = "minefortress-blueprints";
    public static final String BLUEPRINTS_EXTENSION = ".zip";
    public static final String MOD_ID = "minefortress";

    private static final ExecutorService executor;
    private static final ScheduledExecutorService scheduledExecutor;
    public static final TrackedDataHandler<PawnSkin> PAWN_SKIN_TRACKED_DATA_HANDLER = TrackedDataHandler.create(net.minecraft.network.codec.PacketCodecs.indexed(i -> PawnSkin.values()[i], PawnSkin::ordinal));
    public static final TrackedDataHandler<SupportLevel> SUPPORT_LEVEL_TRACKED_DATA_HANDLER = TrackedDataHandler.create(net.minecraft.network.codec.PacketCodecs.indexed(i -> SupportLevel.values()[i], SupportLevel::ordinal));

    static  {
        final var tpIncrementor = new AtomicInteger(0);
        executor = Executors.newCachedThreadPool(r ->
                new Thread(r, "MineFortress Worker " + tpIncrementor.incrementAndGet()));
        final var scheduledTpIncrementor = new AtomicInteger(0);
        scheduledExecutor = Executors.newScheduledThreadPool(1, r ->
                new Thread(r, "MineFortress Scheduled Worker " + scheduledTpIncrementor.incrementAndGet()));

        TrackedDataHandlerRegistry.register(PAWN_SKIN_TRACKED_DATA_HANDLER);
        TrackedDataHandlerRegistry.register(SUPPORT_LEVEL_TRACKED_DATA_HANDLER);
    }

    @Override
    public void onInitialize() {
        FortressServerEvents.register();
        FortressBlocks.register();
        FortressEntities.register();
        FortressItems.register();
        NetworkReaders.register();

        CommandsManager.registerCommands();
        ServerNetworkReceivers.registerReceivers();
        FortressHandledScreensKt.registerHandlerTypes();

        FortressManagerLocator.INSTANCE.register(IServerManagersProvider.class, ServerManagersProvider::new);
        FortressManagerLocator.INSTANCE.register(IServerFortressManager.class, ServerFortressManager::new);
    }

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

}
