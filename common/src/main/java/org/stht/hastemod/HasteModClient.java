package org.stht.hastemod.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.stht.hastemod.HasteMod;
import org.stht.hastemod.client.config.HasteConfig;
import org.stht.hastemod.client.feature.BlockBreaker;

public class HasteModClient {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(HasteMod.MOD_ID, "controls"));
    private static final BlockBreaker BREAKER = new BlockBreaker();

    private static KeyMapping useKey;
    private static KeyMapping toggleKey;
    private static KeyMapping toggleBlockSelKey;

    
    public void onInitializeClient() {
        HasteConfig.get();

        useKey = new KeyMapping(
                "key." + HasteMod.MOD_ID + ".use",
                InputConstants.getKey("key.keyboard.x").getValue(),
                CATEGORY);
        org.stht.hastemod.platform.Services.PLATFORM.registerKeyMapping(useKey);

        toggleKey = new KeyMapping(
                "key." + HasteMod.MOD_ID + ".toggle",
                InputConstants.getKey("key.keyboard.u").getValue(),
                CATEGORY);
        org.stht.hastemod.platform.Services.PLATFORM.registerKeyMapping(toggleKey);

        toggleBlockSelKey = new KeyMapping(
                "key." + HasteMod.MOD_ID + ".toggle_block_sel",
                InputConstants.getKey("key.keyboard.y").getValue(),
                CATEGORY);
        org.stht.hastemod.platform.Services.PLATFORM.registerKeyMapping(toggleBlockSelKey);

        org.stht.hastemod.platform.Services.PLATFORM.registerClientTickEvent(() -> BREAKER.onTick(Minecraft.getInstance()));

        HasteMod.LOGGER.info("Initialized");
    }

    public static KeyMapping getActivateKey() {
        return useKey;
    }

    public static KeyMapping getToggleKey() {
        return toggleKey;
    }

    public static KeyMapping getToggleBlockSelKey() {
        return toggleBlockSelKey;
    }

    public static void onBlockBreak(BlockPos pos) {
        BREAKER.onBlockBreak(pos, Minecraft.getInstance());
    }
}
