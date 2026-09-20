package org.stht.hastemod.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.stht.hastemod.HasteMod;
import org.stht.hastemod.client.HasteModClient;

@Mod(HasteMod.MOD_ID)
public class HasteModNeoForge {
    public HasteModNeoForge(IEventBus modEventBus) {
    }

    @EventBusSubscriber(modid = HasteMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            new HasteModClient().onInitializeClient();
            
            for (var key : NeoForgePlatformHelper.KEY_MAPPINGS) {
                event.register(key);
            }
        }
    }

    @EventBusSubscriber(modid = HasteMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientGameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            for (var tickEvent : NeoForgePlatformHelper.TICK_EVENTS) {
                tickEvent.run();
            }
        }
    }
}
