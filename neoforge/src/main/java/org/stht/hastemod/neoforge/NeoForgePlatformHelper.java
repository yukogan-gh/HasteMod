package org.stht.hastemod.neoforge;

import net.minecraft.client.KeyMapping;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import org.stht.hastemod.platform.services.IPlatformHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NeoForgePlatformHelper implements IPlatformHelper {
    public static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
    public static final List<Runnable> TICK_EVENTS = new ArrayList<>();

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void registerKeyMapping(KeyMapping keyMapping) {
        KEY_MAPPINGS.add(keyMapping);
    }

    @Override
    public void registerClientTickEvent(Runnable onTick) {
        TICK_EVENTS.add(onTick);
    }
}
