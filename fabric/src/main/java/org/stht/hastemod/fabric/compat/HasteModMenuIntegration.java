package org.stht.hastemod.fabric.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.stht.hastemod.client.config.HasteConfigScreen;

@Environment(EnvType.CLIENT)
public class HasteModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return YaclScreenHelper.create();
        }
        return ModMenuApi.super.getModConfigScreenFactory();
    }

    private static class YaclScreenHelper {
        static ConfigScreenFactory<?> create() {
            return HasteConfigScreen::build;
        }
    }
}
