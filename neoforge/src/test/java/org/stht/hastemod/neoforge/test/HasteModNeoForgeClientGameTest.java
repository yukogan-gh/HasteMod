package org.stht.hastemod.neoforge.test;

import com.mojang.blaze3d.platform.InputConstants.Key;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import org.stht.hastemod.client.HasteModClient;
import org.stht.hastemod.client.config.HasteConfig;
import org.stht.hastemod.client.config.HasteConfig.Shape;

@EventBusSubscriber(modid = "hastemod", value = Dist.CLIENT)
public class HasteModNeoForgeClientGameTest {
   private static final AtomicBoolean TEST_STARTED = new AtomicBoolean(false);
   private static int tickCounter = 0;
   private static boolean worldLoaded = false;
   private static boolean worldJoinTriggered = false;

   @SubscribeEvent
   public static void onClientTick(Post event) {
      if (Boolean.getBoolean("hastemod.gametest")) {
         Minecraft mc = Minecraft.getInstance();
         tickCounter++;
         if (tickCounter % 5 == 0) {
            try {
               Object screen = getCurrentScreen(mc);
               if (screen != null) {
                  String screenName = screen.getClass().getName();
                  System.out.println("[GameTest] Screen opened: " + screenName);
                  if (screenName.contains("BackupConfirmScreen")) {
                     System.out.println("[GameTest] BackupConfirmScreen detected. Proceeding without backup...");

                     try {
                        Field onProceedField = screen.getClass().getDeclaredField("onProceed");
                        onProceedField.setAccessible(true);
                        Object listener = onProceedField.get(screen);
                        if (listener != null) {
                           for (Method m : listener.getClass().getDeclaredMethods()) {
                              if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == boolean.class && m.getParameterTypes()[1] == boolean.class) {
                                 m.setAccessible(true);
                                 m.invoke(listener, false, false);
                                 System.out.println("[GameTest] Clicked 'I Know What I'm Doing!' via onProceed");
                                 break;
                              }
                           }
                        }
                     } catch (Exception ex) {
                        System.err.println("[GameTest] Failed to invoke onProceed: " + ex);
                        closeCurrentScreen(mc, screen);
                     }
                  } else if (screenName.contains("LoadingErrorScreen")) {
                     System.out.println("[GameTest] LoadingErrorScreen detected. Triggering nextScreenTask to proceed...");

                     try {
                        Field nstField = screen.getClass().getDeclaredField("nextScreenTask");
                        nstField.setAccessible(true);
                        Runnable task = (Runnable)nstField.get(screen);
                        if (task != null) {
                           task.run();
                        } else {
                           closeCurrentScreen(mc, screen);
                        }
                     } catch (Exception ex) {
                        closeCurrentScreen(mc, screen);
                     }
                  }
               }
            } catch (Exception var13) {
            }
         }

         if (tickCounter >= 10 && mc.level == null && !worldJoinTriggered) {
            Object screen = getCurrentScreen(mc);
            String screenName = screen != null ? screen.getClass().getName() : "null";
            if (screen == null || screenName.contains("TitleScreen") || screenName.contains("LoadingOverlay")) {
               worldJoinTriggered = true;
               System.out.println("[GameTest] Triggering manual openWorld('gametest') from screen=" + screenName);

               try {
                  mc.createWorldOpenFlows().openWorld("gametest", () -> System.out.println("[GameTest] World opened callback invoked!"));
               } catch (Exception ex) {
                  System.err.println("[GameTest] Failed to open world 'gametest': " + ex);
                  ex.printStackTrace();
               }
            }
         }

         if (mc.level != null && mc.player != null && !worldLoaded) {
            worldLoaded = true;
            System.out.println("[GameTest] Player spawned in world: " + mc.player.getName().getString());
            if (TEST_STARTED.compareAndSet(false, true)) {
               Thread testThread = new Thread(() -> runGameTest(mc), "HasteMod-NeoForge-GameTest-Runner");
               testThread.setDaemon(true);
               testThread.start();
            }
         }
      }
   }

   private static Object getCurrentScreen(Minecraft mc) {
      try {
         Field screenField = Minecraft.class.getDeclaredField("screen");
         screenField.setAccessible(true);
         return screenField.get(mc);
      } catch (Exception e) {
         return null;
      }
   }

   private static void closeCurrentScreen(Minecraft mc, Object screen) {
      try {
         Method onClose = screen.getClass().getMethod("onClose");
         onClose.setAccessible(true);
         onClose.invoke(screen);
      } catch (Exception e) {
         try {
            Method setScreen = Minecraft.class.getMethod("setScreen", Screen.class);
            setScreen.setAccessible(true);
            setScreen.invoke(mc, (Object) null);
         } catch (Exception ignored2) {
            try {
               Field screenField = Minecraft.class.getDeclaredField("screen");
               screenField.setAccessible(true);
               screenField.set(mc, null);
            } catch (Exception var5) {
            }
         }
      }
   }

   private static void triggerKeyClick(KeyMapping key) {
      try {
         Field clickCountField = KeyMapping.class.getDeclaredField("clickCount");
         clickCountField.setAccessible(true);
         clickCountField.setInt(key, clickCountField.getInt(key) + 1);
      } catch (Throwable t) {
         try {
            Method clickMethod = KeyMapping.class.getDeclaredMethod("click", Key.class);
            clickMethod.setAccessible(true);
            clickMethod.invoke(null, key.getKey());
         } catch (Throwable t2) {
            t2.printStackTrace();
         }
      }
   }

   private static void waitTicks(int count) throws InterruptedException {
      Thread.sleep(count * 50L);
   }

   private static void runOnClient(Minecraft mc, Runnable r) throws Exception {
      CompletableFuture<Void> f = new CompletableFuture<>();
      mc.execute(() -> {
         try {
            r.run();
            f.complete(null);
         } catch (Throwable t) {
            f.completeExceptionally(t);
         }
      });
      f.get(5L, TimeUnit.SECONDS);
   }

   private static <T> T computeOnClient(Minecraft mc, Callable<T> c) throws Exception {
      CompletableFuture<T> f = new CompletableFuture<>();
      mc.execute(() -> {
         try {
            f.complete(c.call());
         } catch (Throwable t) {
            f.completeExceptionally(t);
         }
      });
      return f.get(5L, TimeUnit.SECONDS);
   }

   private static void runCommand(IntegratedServer server, String cmd) {
      CompletableFuture<Void> future = new CompletableFuture<>();
      server.execute(() -> {
         try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
            future.complete(null);
         } catch (Exception e) {
            future.completeExceptionally(e);
         }
      });

      try {
         future.get(5L, TimeUnit.SECONDS);
      } catch (Exception e) {
         throw new RuntimeException("Command failed: " + cmd, e);
      }
   }

   private static void teleportAndSettle(Minecraft mc, IntegratedServer server, double x, double y, double z) throws Exception {
      runCommand(server, "tp @p " + x + " " + y + " " + z + " 0.0 0.0");

      for (int i = 0; i < 20; i++) {
         waitTicks(1);
         double curZ = computeOnClient(mc, () -> mc.player != null ? mc.player.getZ() : 0.0);
         double curY = computeOnClient(mc, () -> mc.player != null ? mc.player.getY() : 0.0);
         if (Math.abs(curZ - z) < 0.5 && Math.abs(curY - y) < 0.5) {
            break;
         }
      }

      runOnClient(mc, () -> {
         if (mc.player != null) {
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
         }
      });
      waitTicks(2);
   }

   private static void waitForBlockState(Minecraft mc, BlockPos pos, boolean expectAir) throws Exception {
      for (int i = 0; i < 20; i++) {
         waitTicks(1);
         boolean isAir = computeOnClient(mc, () -> mc.level.getBlockState(pos).isAir());
         if (isAir == expectAir) {
            break;
         }
      }
   }

   private static void holdActivateKey(Minecraft mc, int ticks) throws Exception {
      runOnClient(mc, () -> HasteModClient.getActivateKey().setDown(true));

      for (int i = 0; i < ticks; i++) {
         waitTicks(1);
         runOnClient(mc, () -> HasteModClient.getActivateKey().setDown(true));
      }

      runOnClient(mc, () -> HasteModClient.getActivateKey().setDown(false));
      waitTicks(1);
   }

   private static void mineUntilAir(Minecraft mc, BlockPos targetPos, int maxTicks) throws Exception {
      runOnClient(mc, () -> HasteModClient.getActivateKey().setDown(true));

      for (int i = 0; i < maxTicks; i++) {
         waitTicks(1);
         runOnClient(mc, () -> HasteModClient.getActivateKey().setDown(true));
         boolean isAir = computeOnClient(mc, () -> mc.level.getBlockState(targetPos).isAir());
         if (isAir) {
            break;
         }
      }

      runOnClient(mc, () -> HasteModClient.getActivateKey().setDown(false));
      waitTicks(1);
   }

   private static void pressKey(Minecraft mc, KeyMapping key) throws Exception {
      runOnClient(mc, () -> triggerKeyClick(key));
      waitTicks(2);
   }

   private static void runGameTest(Minecraft mc) {
      try {
         System.out.println("[GameTest] =========================================");
         System.out.println("[GameTest]   STARTING HASTEMOD COMPREHENSIVE TEST SUITE");
         System.out.println("[GameTest] =========================================");
         waitTicks(2);
         IntegratedServer server = mc.getSingleplayerServer();
         if (server == null) {
            fail("Singleplayer integrated server is not running");
         }

         testWalkingCorridor(mc, server);
         testToolAutoSwitch(mc, server);
         testBlockSelectionFilter(mc, server);
         testShapeGeometries(mc, server);
         testImmunityAndDisabled(mc, server);
         testTranslationsAndChat(mc);
         System.out.println("[GameTest] =========================================");
         System.out.println("[GameTest]   ALL HASTEMOD GAMETESTS PASSED SUCCESSFULLY!");
         System.out.println("[GameTest] =========================================");
         System.out.flush();
         System.err.flush();
         Runtime.getRuntime().halt(0);
      } catch (Throwable t) {
         t.printStackTrace();
         fail("Test execution encountered an exception: " + t.getMessage());
      }
   }

   private static void testWalkingCorridor(Minecraft mc, IntegratedServer server) throws Exception {
      System.out.println("[GameTest] --- Stage 1: Running 20-Block Walking Corridor Test ---");
      List<String> withoutHaste = List.of("dirt", "sand", "soul_sand", "gravel", "mud", "netherrack", "calcite", "sandstone", "melon", "moss_block");
      List<String> withHaste = List.of(
         "stone", "granite", "diorite", "andesite", "tuff", "dripstone_block", "blackstone", "prismarine", "dark_prismarine", "prismarine_bricks"
      );
      List<String> rowBlocks = new ArrayList<>();
      rowBlocks.addAll(withoutHaste);
      rowBlocks.addAll(withHaste);
      Collections.shuffle(rowBlocks, new Random(12345L));

      for (int z = 0; z <= 22; z++) {
         runCommand(server, "setblock 0 99 " + z + " minecraft:bedrock");
         runCommand(server, "setblock 0 102 " + z + " minecraft:bedrock");
         runCommand(server, "setblock -1 100 " + z + " minecraft:bedrock");
         runCommand(server, "setblock -1 101 " + z + " minecraft:bedrock");
         runCommand(server, "setblock 1 100 " + z + " minecraft:bedrock");
         runCommand(server, "setblock 1 101 " + z + " minecraft:bedrock");
         runCommand(server, "setblock 0 101 " + z + " minecraft:air");
      }

      runCommand(server, "setblock 0 100 0 minecraft:air");

      for (int i = 0; i < rowBlocks.size(); i++) {
         int z = i + 1;
         runCommand(server, "setblock 0 100 " + z + " minecraft:" + rowBlocks.get(i));
      }

      runCommand(server, "setblock 0 100 21 minecraft:air");
      runCommand(server, "setblock 0 100 22 minecraft:air");
      runCommand(server, "gamemode survival @p");
      runCommand(server, "effect give @p minecraft:saturation 1000 255 true");
      runCommand(server, "effect give @p minecraft:haste 1000 1 true");
      runCommand(server, "effect give @p minecraft:speed 1000 2 true");
      runCommand(server, "item replace entity @p hotbar.0 with minecraft:netherite_pickaxe[enchantments={\"minecraft:efficiency\":5}] 1");
      runCommand(server, "item replace entity @p hotbar.1 with minecraft:netherite_axe[enchantments={\"minecraft:efficiency\":5}] 1");
      runCommand(server, "item replace entity @p hotbar.2 with minecraft:netherite_shovel[enchantments={\"minecraft:efficiency\":5}] 1");
      runCommand(server, "item replace entity @p hotbar.3 with minecraft:netherite_hoe[enchantments={\"minecraft:efficiency\":5}] 1");
      runCommand(server, "item replace entity @p hotbar.4 with minecraft:shears[enchantments={\"minecraft:efficiency\":5}] 1");
      teleportAndSettle(mc, server, 0.5, 100.0, 0.5);
      runOnClient(mc, () -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.CUBE;
         cfg.radius = 4;
         cfg.blocksPerTick = 10;
         cfg.tickDelay = 0;
      });

      for (int i = 0; i < 20; i++) {
         Field f = HasteModClient.class.getDeclaredField("BREAKER");
         f.setAccessible(true);
         Object breaker = f.get(null);
         Field ef = breaker.getClass().getDeclaredField("enabled");
         ef.setAccessible(true);
         boolean isEnabled = ef.getBoolean(breaker);
         if (isEnabled) {
            break;
         }

         pressKey(mc, HasteModClient.getToggleKey());
      }

      KeyMapping activateKey = HasteModClient.getActivateKey();
      KeyMapping upKey = mc.options.keyUp;
      runOnClient(mc, () -> {
         activateKey.setDown(true);
         upKey.setDown(true);
      });
      double currentZ = 0.0;

      for (int tick = 0; tick < 200; tick++) {
         waitTicks(1);
         runOnClient(mc, () -> {
            activateKey.setDown(true);
            upKey.setDown(true);
         });
         currentZ = computeOnClient(mc, () -> mc.player != null ? mc.player.getZ() : 0.0);
         if (currentZ >= 20.5) {
            break;
         }
      }

      runOnClient(mc, () -> {
         activateKey.setDown(false);
         upKey.setDown(false);
      });
      waitTicks(1);
      if (currentZ < 20.0) {
         fail("Corridor test failed: Player blocked at Z=" + currentZ);
      }

      for (int i = 0; i < rowBlocks.size(); i++) {
         BlockPos pos = new BlockPos(0, 100, i + 1);
         boolean isAir = computeOnClient(mc, () -> mc.level.getBlockState(pos).isAir());
         if (!isAir) {
            fail("Corridor block at " + pos + " (" + rowBlocks.get(i) + ") was NOT broken!");
         }
      }

      System.out.println("[GameTest] Stage 1 PASSED: Player traversed corridor and mined all 20 blocks.");
   }

   private static void testToolAutoSwitch(Minecraft mc, IntegratedServer server) throws Exception {
      System.out.println("[GameTest] --- Stage 2: Testing Tool Auto-Switching ---");
      teleportAndSettle(mc, server, 0.5, 100.0, 3.5);
      runOnClient(mc, () -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.CUBE;
         cfg.radius = 3;
         cfg.blocksPerTick = 1;
         cfg.tickDelay = 0;
      });
      BlockPos targetPos = new BlockPos(0, 100, 5);
      runOnClient(mc, () -> mc.player.getInventory().setSelectedSlot(0));
      runCommand(server, "setblock 0 100 5 dirt");
      waitForBlockState(mc, targetPos, false);
      mineUntilAir(mc, targetPos, 10);
      boolean dirtBroken = computeOnClient(mc, () -> mc.level.getBlockState(targetPos).isAir());
      int slotAfterDirt = computeOnClient(mc, () -> mc.player.getInventory().getSelectedSlot());
      if (!dirtBroken) {
         fail("Tool auto-switch: Dirt block at " + targetPos + " was not broken!");
      }

      if (slotAfterDirt != 2) {
         fail("Tool auto-switch: Expected slot 2 (Shovel) for dirt, but held slot " + slotAfterDirt);
      }

      runCommand(server, "setblock 0 100 5 melon");
      waitForBlockState(mc, targetPos, false);
      mineUntilAir(mc, targetPos, 10);
      boolean melonBroken = computeOnClient(mc, () -> mc.level.getBlockState(targetPos).isAir());
      int slotAfterMelon = computeOnClient(mc, () -> mc.player.getInventory().getSelectedSlot());
      if (!melonBroken) {
         fail("Tool auto-switch: Melon block at " + targetPos + " was not broken!");
      }

      if (slotAfterMelon != 1) {
         fail("Tool auto-switch: Expected slot 1 (Axe) for melon, but held slot " + slotAfterMelon);
      }

      runCommand(server, "setblock 0 100 5 stone");
      waitForBlockState(mc, targetPos, false);
      mineUntilAir(mc, targetPos, 10);
      boolean stoneBroken = computeOnClient(mc, () -> mc.level.getBlockState(targetPos).isAir());
      int slotAfterStone = computeOnClient(mc, () -> mc.player.getInventory().getSelectedSlot());
      if (!stoneBroken) {
         fail("Tool auto-switch: Stone block at " + targetPos + " was not broken!");
      }

      if (slotAfterStone != 0) {
         fail("Tool auto-switch: Expected slot 0 (Pickaxe) for stone, but held slot " + slotAfterStone);
      }

      System.out.println("[GameTest] Stage 2 PASSED: Tool auto-switching dynamically selected Pickaxe, Axe, and Shovel.");
   }

   private static void testBlockSelectionFilter(Minecraft mc, IntegratedServer server) throws Exception {
      System.out.println("[GameTest] --- Stage 3: Testing Block Selection Filter Mode ---");
      teleportAndSettle(mc, server, 0.5, 100.0, 3.5);
      runOnClient(mc, () -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.CUBE;
         cfg.radius = 3;
         cfg.blocksPerTick = 10;
         cfg.tickDelay = 0;
      });
      pressKey(mc, HasteModClient.getToggleBlockSelKey());
      BlockPos samplePos = new BlockPos(0, 100, 4);
      runCommand(server, "setblock 0 100 4 stone");
      waitForBlockState(mc, samplePos, false);
      runOnClient(mc, () -> HasteModClient.onBlockBreak(samplePos));
      runCommand(server, "setblock 0 100 4 air");
      waitForBlockState(mc, samplePos, true);
      runCommand(server, "setblock 0 100 5 stone");
      runCommand(server, "setblock 1 100 5 stone");
      runCommand(server, "setblock 0 100 6 dirt");
      runCommand(server, "setblock -1 100 5 dirt");
      waitForBlockState(mc, new BlockPos(0, 100, 5), false);
      mineUntilAir(mc, new BlockPos(0, 100, 5), 10);
      boolean stone1Air = computeOnClient(mc, () -> mc.level.getBlockState(new BlockPos(0, 100, 5)).isAir());
      boolean stone2Air = computeOnClient(mc, () -> mc.level.getBlockState(new BlockPos(1, 100, 5)).isAir());
      if (!stone1Air || !stone2Air) {
         fail("Block selection filter: Matching stone blocks were not broken!");
      }

      boolean dirt1Preserved = computeOnClient(mc, () -> mc.level.getBlockState(new BlockPos(0, 100, 6)).is(Blocks.DIRT));
      boolean dirt2Preserved = computeOnClient(mc, () -> mc.level.getBlockState(new BlockPos(-1, 100, 5)).is(Blocks.DIRT));
      if (!dirt1Preserved || !dirt2Preserved) {
         fail("Block selection filter FAILED: Unselected dirt blocks were mined!");
      }

      pressKey(mc, HasteModClient.getToggleBlockSelKey());
      mineUntilAir(mc, new BlockPos(0, 100, 6), 10);
      boolean dirt1NowAir = computeOnClient(mc, () -> mc.level.getBlockState(new BlockPos(0, 100, 6)).isAir());
      boolean dirt2NowAir = computeOnClient(mc, () -> mc.level.getBlockState(new BlockPos(-1, 100, 5)).isAir());
      if (!dirt1NowAir || !dirt2NowAir) {
         fail("Block selection filter: Dirt blocks were not mined after filter mode was toggled OFF!");
      }

      System.out.println("[GameTest] Stage 3 PASSED: Block selection mode selectively filtered and preserved unselected blocks.");
   }

   private static void testShapeGeometries(Minecraft mc, IntegratedServer server) throws Exception {
      System.out.println("[GameTest] --- Stage 4: Testing Shape Geometries (TUNNEL and LAYER) ---");
      teleportAndSettle(mc, server, 0.5, 100.0, 3.5);
      runOnClient(mc, () -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.TUNNEL;
         cfg.radius = 3;
         cfg.blocksPerTick = 25;
         cfg.tickDelay = 0;
      });
      runCommand(server, "fill -2 100 5 2 104 5 stone");
      waitForBlockState(mc, new BlockPos(0, 100, 5), false);
      mineUntilAir(mc, new BlockPos(0, 100, 5), 15);

      for (int x = -1; x <= 1; x++) {
         for (int y = 100; y <= 102; y++) {
            BlockPos pos = new BlockPos(x, y, 5);
            boolean isAir = computeOnClient(mc, () -> mc.level.getBlockState(pos).isAir());
            if (!isAir) {
               fail("TUNNEL shape: Block inside 3x3 tunnel window at " + pos + " was NOT mined!");
            }
         }
      }

      BlockPos leftWall = new BlockPos(-2, 101, 5);
      BlockPos rightWall = new BlockPos(2, 101, 5);
      BlockPos ceiling = new BlockPos(0, 103, 5);
      boolean leftIntact = computeOnClient(mc, () -> mc.level.getBlockState(leftWall).is(Blocks.STONE));
      boolean rightIntact = computeOnClient(mc, () -> mc.level.getBlockState(rightWall).is(Blocks.STONE));
      boolean ceilIntact = computeOnClient(mc, () -> mc.level.getBlockState(ceiling).is(Blocks.STONE));
      if (!leftIntact || !rightIntact || !ceilIntact) {
         fail("TUNNEL shape: Blocks outside tunnel geometry were mined!");
      }

      runCommand(server, "fill -2 100 5 2 104 5 air");
      waitTicks(2);
      teleportAndSettle(mc, server, 0.5, 100.0, 3.5);
      runOnClient(mc, () -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.LAYER;
         cfg.radius = 3;
         cfg.blocksPerTick = 10;
         cfg.tickDelay = 0;
      });
      runCommand(server, "setblock 0 100 5 stone");
      runCommand(server, "setblock 0 101 5 stone");
      waitForBlockState(mc, new BlockPos(0, 100, 5), false);
      mineUntilAir(mc, new BlockPos(0, 100, 5), 10);
      boolean layerBroken = computeOnClient(mc, () -> mc.level.getBlockState(new BlockPos(0, 100, 5)).isAir());
      boolean aboveUntouched = computeOnClient(mc, () -> mc.level.getBlockState(new BlockPos(0, 101, 5)).is(Blocks.STONE));
      if (!layerBroken) {
         fail("LAYER shape: Block at player level (0, 100, 5) was not broken!");
      }

      if (!aboveUntouched) {
         fail("LAYER shape: Block above player level (0, 101, 5) was improperly broken!");
      }

      runCommand(server, "setblock 0 101 5 air");
      waitTicks(1);
      System.out.println("[GameTest] Stage 4 PASSED: TUNNEL and LAYER shapes respected geometric constraints.");
   }

   private static void testImmunityAndDisabled(Minecraft mc, IntegratedServer server) throws Exception {
      System.out.println("[GameTest] --- Stage 5: Testing Non-Instamineable Immunity & Disabled State ---");
      teleportAndSettle(mc, server, 0.5, 100.0, 3.5);
      runOnClient(mc, () -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.CUBE;
         cfg.radius = 3;
         cfg.blocksPerTick = 10;
         cfg.tickDelay = 0;
      });
      BlockPos targetPos = new BlockPos(0, 100, 5);
      runCommand(server, "setblock 0 100 5 obsidian");
      waitForBlockState(mc, targetPos, false);
      holdActivateKey(mc, 5);
      boolean obsidianPreserved = computeOnClient(mc, () -> mc.level.getBlockState(targetPos).is(Blocks.OBSIDIAN));
      if (!obsidianPreserved) {
         fail("Non-instamineable immunity FAILED: Obsidian was broken!");
      }

      runCommand(server, "setblock 0 100 5 air");
      waitForBlockState(mc, targetPos, true);
      pressKey(mc, HasteModClient.getToggleKey());
      waitTicks(2);
      runCommand(server, "setblock 0 100 5 stone");
      waitForBlockState(mc, targetPos, false);
      holdActivateKey(mc, 5);
      boolean stoneUntouchedWhileDisabled = computeOnClient(mc, () -> mc.level.getBlockState(targetPos).is(Blocks.STONE));
      if (!stoneUntouchedWhileDisabled) {
         fail("Disabled state FAILED: Block was broken while HasteMod was disabled!");
      }

      pressKey(mc, HasteModClient.getToggleKey());
      waitTicks(2);
      mineUntilAir(mc, targetPos, 10);
      boolean stoneBrokenAfterReEnabled = computeOnClient(mc, () -> mc.level.getBlockState(targetPos).isAir());
      if (!stoneBrokenAfterReEnabled) {
         fail("Disabled state: Block was not broken after re-enabling HasteMod!");
      }

      runCommand(server, "setblock 0 100 5 air");
      waitTicks(1);
      System.out.println("[GameTest] Stage 5 PASSED: Non-instamineable obsidian preserved; toggled-off state respected.");
   }

   private static void testTranslationsAndChat(Minecraft mc) throws Exception {
      System.out.println("[GameTest] --- Stage 6: Validating Translation Keys & Chat Messages ---");
      runOnClient(
         mc,
         () -> {
            Language lang = Language.getInstance();
            String[] requiredKeys = new String[]{
               "key.category.hastemod.controls",
               "key.category.hastemod",
               "key.categories.hastemod",
               "key.hastemod.use",
               "key.hastemod.toggle",
               "key.hastemod.toggle_block_sel",
               "text.hastemod.category.breaking",
               "text.hastemod.group.shape",
               "text.hastemod.group.throttle",
               "text.hastemod.group.throttle.tooltip",
               "text.hastemod.option.shape",
               "text.hastemod.option.shape.tooltip",
               "text.hastemod.option.radius",
               "text.hastemod.option.radius.tooltip",
               "text.hastemod.option.blocks_per_tick",
               "text.hastemod.option.blocks_per_tick.tooltip",
               "text.hastemod.option.tick_delay",
               "text.hastemod.option.tick_delay.tooltip",
               "msg.hastemod.toggled_on",
               "msg.hastemod.toggled_off",
               "msg.hastemod.block_sel_toggled_on",
               "msg.hastemod.block_sel_toggled_off",
               "msg.hastemod.selected_block"
            };

            for (String key : requiredKeys) {
               if (!lang.has(key)) {
                  fail("Missing translation key in Language instance: " + key);
               }

               String localized = Component.translatable(key).getString();
               if (localized.isEmpty() || localized.equals(key)) {
                  fail("Translation key returned raw key or empty string: " + key + " -> '" + localized + "'");
               }
            }

            try {
               Object chatComponent = getChatComponent(mc);
               if (chatComponent != null) {
                  Field allMessagesField = null;

                  for (Field f : chatComponent.getClass().getDeclaredFields()) {
                     if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        allMessagesField = f;
                        break;
                     }
                  }

                  if (allMessagesField != null) {
                     List<?> messages = (List<?>)allMessagesField.get(chatComponent);
                     if (messages != null) {
                        for (Object msg : messages) {
                           String text = msg.toString();
                           if (text.contains("msg.hastemod") || text.contains("key.hastemod") || text.contains("text.hastemod")) {
                              fail("Found raw unlocalized key in chat message: " + text);
                           }
                        }
                     }
                  }
               }
            } catch (Throwable e) {
               if (e instanceof RuntimeException re && re.getMessage() != null && re.getMessage().contains("[GameTest FAILED]")) {
                  throw re;
               }

               System.err.println("[GameTest] Note: Could not introspect chat messages via reflection: " + e.getMessage());
            }
         }
      );
      System.out.println("[GameTest] Stage 6 PASSED: All translation keys and chat messages validated.");
   }

   private static Object getChatComponent(Minecraft client) {
      if (client.gui == null) {
         return null;
      }

      try {
         Method m = client.gui.getClass().getMethod("getChat");
         return m.invoke(client.gui);
      } catch (Throwable t) {
         for (Method m : client.gui.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().toLowerCase().contains("chat")) {
               try {
                  return m.invoke(client.gui);
               } catch (ReflectiveOperationException var7) {
               }
            }
         }

         return null;
      }
   }

   private static void fail(String message) {
      System.err.println("\n=======================================================");
      System.err.println(" [GameTest FAILED] " + message);
      System.err.println("=======================================================");
      System.out.flush();
      System.err.flush();
      Runtime.getRuntime().halt(1);
   }
}
