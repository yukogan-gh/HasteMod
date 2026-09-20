package org.stht.hastemod.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.stht.hastemod.client.config.HasteConfigScreen;

@Environment(EnvType.CLIENT)
public class HasteModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HasteConfigScreen::build;
    }
}
