package org.stht.hastemod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HasteModClient implements ClientModInitializer {
    private static KeyBinding useKey;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        useKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Use",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "HasteMod"
        ));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Toggle on/off",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "HasteMod"
        ));


        BlockBreaker bbreaker = new BlockBreaker();
        ClientTickEvents.END_CLIENT_TICK.register((tick) -> bbreaker.onTick(MinecraftClient.getInstance()));
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> bbreaker.onBlockBreak(state, MinecraftClient.getInstance()));

        System.out.println("Initialized HasteMod");
    }

    public static KeyBinding getActivateKey() {
        return useKey;
    }

    public static KeyBinding getToggleKey() {
        return toggleKey;
    }

}
