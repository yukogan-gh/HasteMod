package org.stht.hastemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.stht.hastemod.HasteMod;
import org.stht.hastemod.client.config.HasteConfig;
import org.stht.hastemod.client.feature.BlockBreaker;

public class HasteModClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(HasteMod.MOD_ID, "controls"));
    private static final BlockBreaker BREAKER = new BlockBreaker();

    private static KeyMapping useKey;
    private static KeyMapping toggleKey;
    private static KeyMapping toggleBlockSelKey;

    @Override
    public void onInitializeClient() {
        HasteConfig.get();

        useKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + HasteMod.MOD_ID + ".use",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                CATEGORY));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + HasteMod.MOD_ID + ".toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                CATEGORY));

        toggleBlockSelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + HasteMod.MOD_ID + ".toggle_block_sel",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(BREAKER::onTick);

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
