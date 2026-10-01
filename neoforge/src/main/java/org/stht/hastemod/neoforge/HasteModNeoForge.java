package org.stht.hastemod.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.stht.hastemod.HasteMod;
import org.stht.hastemod.client.HasteModClient;
import org.stht.hastemod.client.config.HasteConfigScreen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod(value = HasteMod.MOD_ID, dist = Dist.CLIENT)
public class HasteModNeoForge {
    public HasteModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        if (isClient()) {
            modEventBus.addListener(ClientEvents::onKeyRegister);
            NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
            if (ModList.get().isLoaded("yet_another_config_lib_v3")) {
                NeoForgeYaclCompat.registerConfigScreen(modContainer);
            }
        }
    }

    private static boolean isClient() {
        try {
            Method m = FMLEnvironment.class.getMethod("getDist");
            Object dist = m.invoke(null);
            return dist == Dist.CLIENT;
        } catch (Throwable ignored) {
            try {
                Field f = FMLEnvironment.class.getField("dist");
                Object dist = f.get(null);
                return dist == Dist.CLIENT;
            } catch (Throwable ignored2) {
                return true;
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

    public static class ClientEvents {
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            new HasteModClient().onInitializeClient();

            for (var key : NeoForgePlatformHelper.KEY_MAPPINGS) {
                event.register(key);
            }
        }

        public static void onClientTick(ClientTickEvent.Post event) {
            for (var tickEvent : NeoForgePlatformHelper.TICK_EVENTS) {
                tickEvent.run();
            }
        }
    }
}
