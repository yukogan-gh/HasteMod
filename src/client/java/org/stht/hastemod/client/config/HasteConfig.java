package org.stht.hastemod.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.stht.hastemod.HasteMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Environment(EnvType.CLIENT)
public class HasteConfig {
    public enum Shape {
        CUBE,
        SPHERE,
        LAYER,
        TUNNEL
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(HasteMod.MOD_ID + ".json");

    private static HasteConfig instance;

    public Shape shape = Shape.CUBE;
    public int radius = 4;
    public int blocksPerTick = 16;
    public int tickDelay = 2;

    public static HasteConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static HasteConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                HasteConfig loaded = GSON.fromJson(json, HasteConfig.class);
                if (loaded != null) return loaded.sanitized();
            }
        } catch (IOException | RuntimeException e) {
            HasteMod.LOGGER.error("Failed to load config", e);
        }
        HasteConfig fresh = new HasteConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            HasteMod.LOGGER.error("Failed to save config", e);
        }
    }

    private HasteConfig sanitized() {
        if (shape == null) shape = Shape.CUBE;
        radius = Math.clamp(radius, 1, 5);
        blocksPerTick = Math.clamp(blocksPerTick, 1, 256);
        tickDelay = Math.clamp(tickDelay, 0, 200);
        return this;
    }
}
