package org.stht.hastemod.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.stht.hastemod.HasteMod;
import org.stht.hastemod.client.HasteModClient;
import org.stht.hastemod.client.config.HasteConfigScreen;

@Mod(HasteMod.MOD_ID)
public class HasteModNeoForge {
    public HasteModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.getDist().isClient()) {
            if (ModList.get().isLoaded("yet_another_config_lib_v3")) {
                NeoForgeYaclCompat.registerConfigScreen(modContainer);
            }
        }
    }

    private static class NeoForgeYaclCompat {
        static void registerConfigScreen(ModContainer modContainer) {
            modContainer.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (container, parentScreen) -> HasteConfigScreen.build(parentScreen)
            );
        }
    }

    @EventBusSubscriber(modid = HasteMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            new HasteModClient().onInitializeClient();

            for (var key : NeoForgePlatformHelper.KEY_MAPPINGS) {
                event.register(key);
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            for (var tickEvent : NeoForgePlatformHelper.TICK_EVENTS) {
                tickEvent.run();
            }
        }
    }
}
