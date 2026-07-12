package net.mehvahdjukaar.vista.platform;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.chunk_tracking.ServerCameraChunkManager;
import net.mehvahdjukaar.vista.configs.CommonConfigs;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.create.CreateCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.ref.WeakReference;

import static net.mehvahdjukaar.vista.VistaMod.MOD_ID;

/**
 * Author: MehVahdJukaar
 */
@Mod(MOD_ID)
public class VistaForge {

    public static WeakReference<IEventBus> modBus;

    public VistaForge(IEventBus bus) {
        modBus = new WeakReference<>(bus);
        VistaMod.init();
        CreateCompat.registerNetwork();
        bus.addListener(this::registerCapabilities);
        bus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    //PlatHelper.addCommonSetup in common
    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Create's interaction registry isn't thread-safe, so register on the main thread
        if (CompatHandler.CREATE) event.enqueueWork(CreateCompat::setup);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (!CommonConfigs.TV_CONSUME_ENERGY.get()) return;
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                VistaMod.TV_TILE.get(),
                (tv, side) -> TvEnergyHandler.getOrCreate(tv));

    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            VistaMod.addEntityGoal(event.getEntity(), serverLevel);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ServerCameraChunkManager.onPlayerLeave(sp);
        }
    }

    @SubscribeEvent
    public void onServerPlayerTick(PlayerTickEvent.Post event) {
        Player entity = event.getEntity();
        if (entity instanceof ServerPlayer sp) VistaMod.onServerPlayerTick(sp);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ServerCameraChunkManager.clearAll(event.getServer());
    }

}
