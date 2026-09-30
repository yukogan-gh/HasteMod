package org.stht.hastemod.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.stht.hastemod.platform.services.IPlatformHelper;
import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void registerKeyMapping(KeyMapping keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }

    @Override
    public void registerClientTickEvent(Runnable onTick) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick.run());
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
