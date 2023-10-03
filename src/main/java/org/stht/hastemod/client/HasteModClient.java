package org.stht.hastemod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class HasteModClient implements ClientModInitializer {
    private static KeyBinding useKey;
    private static KeyBinding toggleKey;
    private static KeyBinding toggleBlockSelKey;
    private static BlockBreaker bbreaker = new BlockBreaker();

    @Override
    public void onInitializeClient() {
        bbreaker = new BlockBreaker();

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

        toggleBlockSelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "Toggle Block Selection Mode on/off",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_Y,
        "HasteMod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register((tick) -> bbreaker.onTick(MinecraftClient.getInstance()));
        //PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> bbreaker.onBlockBreak(state, MinecraftClient.getInstance()));

        System.out.println("Initialized HasteMod");
    }

    public static KeyBinding getActivateKey() {
        return useKey;
    }

    public static KeyBinding getToggleKey() {
        return toggleKey;
    }

    public static KeyBinding getToggleBlockSelKey() {
        return toggleBlockSelKey;
    }

    public static void onBlockBreak(BlockPos pos) {
        bbreaker.onBlockBreak(pos, MinecraftClient.getInstance());
    }

}
