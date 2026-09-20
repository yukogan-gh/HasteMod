package org.stht.hastemod.fabric;

import net.fabricmc.api.ClientModInitializer;
import org.stht.hastemod.client.HasteModClient;

public class FabricHasteModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new HasteModClient().onInitializeClient();
    }
}
