package org.stht.hastemod.platform.services;

import net.minecraft.client.KeyMapping;
import java.nio.file.Path;

public interface IPlatformHelper {
    Path getConfigDir();

    void registerKeyMapping(KeyMapping keyMapping);

    void registerClientTickEvent(Runnable onTick);

    default boolean isModLoaded(String modId) {
        return false;
    }
}
