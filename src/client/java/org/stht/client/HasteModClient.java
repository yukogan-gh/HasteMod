package org.stht.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class HasteModClient implements ClientModInitializer {
    private static KeyMapping useKey;
    private static KeyMapping toggleKey;
    private static KeyMapping toggleBlockSelKey;
    private static BlockBreaker bbreaker = new BlockBreaker();
    private static KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("hastemod", "controls"));

    @Override
    public void onInitializeClient() {
        bbreaker = new BlockBreaker();

        useKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "Use",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                category
        ));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "Toggle on/off",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                category
        ));

        toggleBlockSelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "Toggle Block Selection Mode on/off",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register((tick) -> bbreaker.onTick(Minecraft.getInstance()));/*
        ClientTickEvents.START_CLIENT_TICK.register((tick) -> {
            if (!bbreaker.isBreaking || MinecraftClient.getInstance().player == null) return;
            bbreaker.isBreaking = false;
            MinecraftClient.getInstance().interactionManager.cancelBlockBreaking();
        });*/

        System.out.println("Initialized HasteMod");
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
        bbreaker.onBlockBreak(pos, Minecraft.getInstance());
    }

}
