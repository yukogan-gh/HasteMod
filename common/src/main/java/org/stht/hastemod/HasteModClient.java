package org.stht.hastemod.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.stht.hastemod.HasteMod;
import org.stht.hastemod.client.config.HasteConfig;
import org.stht.hastemod.client.feature.BlockBreaker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class HasteModClient {
    private static final String CATEGORY = "key.category.hastemod.controls";
    private static final BlockBreaker BREAKER = new BlockBreaker();

    private static KeyMapping useKey;
    private static KeyMapping toggleKey;
    private static KeyMapping toggleBlockSelKey;
    private static Object categoryObject = null;

    public void onInitializeClient() {
        HasteConfig.get();

        useKey = createKeyMapping(
                "key." + HasteMod.MOD_ID + ".use",
                InputConstants.getKey("key.keyboard.x").getValue(),
                CATEGORY);
        org.stht.hastemod.platform.Services.PLATFORM.registerKeyMapping(useKey);

        toggleKey = createKeyMapping(
                "key." + HasteMod.MOD_ID + ".toggle",
                InputConstants.getKey("key.keyboard.u").getValue(),
                CATEGORY);
        org.stht.hastemod.platform.Services.PLATFORM.registerKeyMapping(toggleKey);

        toggleBlockSelKey = createKeyMapping(
                "key." + HasteMod.MOD_ID + ".toggle_block_sel",
                InputConstants.getKey("key.keyboard.y").getValue(),
                CATEGORY);
        org.stht.hastemod.platform.Services.PLATFORM.registerKeyMapping(toggleBlockSelKey);

        org.stht.hastemod.platform.Services.PLATFORM.registerClientTickEvent(() -> BREAKER.onTick(Minecraft.getInstance()));

        HasteMod.LOGGER.info("Initialized");
    }

    private static KeyMapping createKeyMapping(String name, int keyCode, String categoryString) {
        for (Constructor<?> ctor : KeyMapping.class.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 3 && params[0] == String.class && (params[1] == int.class || params[1] == Integer.class)) {
                if (params[2] == String.class) {
                    try {
                        return (KeyMapping) ctor.newInstance(name, keyCode, categoryString);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to construct KeyMapping with String category", e);
                    }
                } else {
                    Class<?> categoryClass = params[2];
                    if (categoryObject == null) {
                        categoryObject = resolveCategory(categoryClass);
                    }
                    try {
                        return (KeyMapping) ctor.newInstance(name, keyCode, categoryObject);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to construct KeyMapping with Category", e);
                    }
                }
            }
        }
        throw new IllegalStateException("No suitable KeyMapping constructor found on " + KeyMapping.class.getName());
    }

    private static Object resolveCategory(Class<?> categoryClass) {
        for (Method m : categoryClass.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == categoryClass && m.getParameterCount() == 1) {
                Class<?> idClass = m.getParameterTypes()[0];
                Object idObj = createIdentifier(idClass, HasteMod.MOD_ID, "controls");
                try {
                    return m.invoke(null, idObj);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke category registration method", e);
                }
            }
        }
        for (Constructor<?> c : categoryClass.getConstructors()) {
            if (c.getParameterCount() == 1) {
                Class<?> idClass = c.getParameterTypes()[0];
                Object idObj = createIdentifier(idClass, HasteMod.MOD_ID, "controls");
                try {
                    return c.newInstance(idObj);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to construct KeyMapping Category", e);
                }
            }
        }
        throw new IllegalStateException("Could not create or register Category for " + categoryClass.getName());
    }

    private static Object createIdentifier(Class<?> idClass, String namespace, String path) {
        for (Method m : idClass.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == idClass) {
                if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == String.class && m.getParameterTypes()[1] == String.class) {
                    try {
                        return m.invoke(null, namespace, path);
                    } catch (Exception ignored) {}
                }
            }
        }
        String fullId = namespace + ":" + path;
        for (Method m : idClass.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == idClass) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                    try {
                        return m.invoke(null, fullId);
                    } catch (Exception ignored) {}
                }
            }
        }
        for (Constructor<?> ctor : idClass.getConstructors()) {
            if (ctor.getParameterCount() == 2 && ctor.getParameterTypes()[0] == String.class && ctor.getParameterTypes()[1] == String.class) {
                try {
                    return ctor.newInstance(namespace, path);
                } catch (Exception ignored) {}
            }
        }
        for (Constructor<?> ctor : idClass.getConstructors()) {
            if (ctor.getParameterCount() == 1 && ctor.getParameterTypes()[0] == String.class) {
                try {
                    return ctor.newInstance(fullId);
                } catch (Exception ignored) {}
            }
        }
        throw new RuntimeException("Failed to instantiate identifier " + idClass.getName());
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
