package org.stht.hastemod.fabric.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import org.stht.hastemod.client.HasteModClient;
import org.stht.hastemod.client.config.HasteConfig;
import org.stht.hastemod.client.config.HasteConfig.Shape;

public class HasteModClientGameTest implements FabricClientGameTest {
   public void runTest(ClientGameTestContext context) {
      System.out.println("\n[GameTest] =========================================");
      System.out.println("[GameTest]   STARTING HASTEMOD COMPREHENSIVE TEST SUITE");
      System.out.println("[GameTest] =========================================");

      try {
         TestSingleplayerContext singleplayer = context.worldBuilder().create();

         try {
            context.waitTicks(5);
            setupEnvironment(singleplayer);
            context.waitTicks(5);
            testWalkingCorridor(context, singleplayer);
            prepareTestingZone(context, singleplayer);
            testToolAutoSwitch(context, singleplayer);
            testBlockSelectionFilter(context, singleplayer);
            testShapeGeometries(context, singleplayer);
            testImmunityAndDisabled(context, singleplayer);
            testTranslationsAndChat(context);
            System.out.println("\n[GameTest] =========================================");
            System.out.println("[GameTest]   ALL HASTEMOD GAMETESTS PASSED SUCCESSFULLY!");
            System.out.println("[GameTest] =========================================\n");
            System.out.flush();
            System.err.flush();
            Runtime.getRuntime().halt(0);
         } catch (Throwable var6) {
            if (singleplayer != null) {
               try {
                  singleplayer.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (singleplayer != null) {
            singleplayer.close();
         }
      } catch (Throwable t) {
         t.printStackTrace();
         System.err.println("\n=======================================================");
         System.err.println(" [GameTest FAILED] " + t.getMessage());
         System.err.println("=======================================================");
         System.out.flush();
         System.err.flush();
         Runtime.getRuntime().halt(1);
      }
   }

   private static void setupEnvironment(TestSingleplayerContext singleplayer) {
      singleplayer.getServer().runCommand("gamerule spawn_mobs false");
      singleplayer.getServer().runCommand("gamerule advance_time false");
      singleplayer.getServer().runCommand("gamemode survival @p");
      singleplayer.getServer().runCommand("fill -3 99 0 3 99 22 bedrock");
      singleplayer.getServer().runCommand("fill -3 100 0 3 105 22 air");
      singleplayer.getServer().runCommand("item replace entity @p hotbar.0 with netherite_pickaxe[enchantments={\"minecraft:efficiency\":5}] 1");
      singleplayer.getServer().runCommand("item replace entity @p hotbar.1 with netherite_axe[enchantments={\"minecraft:efficiency\":5}] 1");
      singleplayer.getServer().runCommand("item replace entity @p hotbar.2 with netherite_shovel[enchantments={\"minecraft:efficiency\":5}] 1");
      singleplayer.getServer().runCommand("item replace entity @p hotbar.3 with netherite_hoe[enchantments={\"minecraft:efficiency\":5}] 1");
      singleplayer.getServer().runCommand("effect give @p haste 1000 1 true");
      singleplayer.getServer().runCommand("effect give @p saturation 1000 255 true");
      singleplayer.getServer().runCommand("effect give @p speed 1000 2 true");
   }

   private static void prepareTestingZone(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
      singleplayer.getServer().runCommand("fill -3 100 0 3 104 22 air");
      singleplayer.getServer().runCommand("fill -3 99 0 3 99 15 bedrock");
      context.waitTicks(3);
   }

   private static void teleportAndSettle(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, double y, double z) {
      singleplayer.getServer().runCommand("teleport @p " + x + " " + y + " " + z + " 0.0 0.0");

      for (int i = 0; i < 20; i++) {
         context.waitTick();
         double curZ = (Double)context.computeOnClient(client -> client.player != null ? client.player.getZ() : 0.0);
         double curY = (Double)context.computeOnClient(client -> client.player != null ? client.player.getY() : 0.0);
         if (Math.abs(curZ - z) < 0.5 && Math.abs(curY - y) < 0.5) {
            break;
         }
      }

      context.runOnClient(client -> {
         if (client.player != null) {
            client.player.setDeltaMovement(0.0, 0.0, 0.0);
         }
      });
      context.waitTicks(2);
   }

   private static void waitForBlockState(ClientGameTestContext context, BlockPos pos, boolean expectAir) {
      for (int i = 0; i < 20; i++) {
         context.waitTick();
         boolean isAir = (Boolean)context.computeOnClient(client -> client.level.getBlockState(pos).isAir());
         if (isAir == expectAir) {
            break;
         }
      }
   }

   private static void holdActivateKey(ClientGameTestContext context, int ticks) {
      context.runOnClient(client -> HasteModClient.getActivateKey().setDown(true));
      context.getInput().holdKey(HasteModClient.getActivateKey());

      for (int i = 0; i < ticks; i++) {
         context.waitTick();
         context.runOnClient(client -> HasteModClient.getActivateKey().setDown(true));
      }

      context.runOnClient(client -> HasteModClient.getActivateKey().setDown(false));
      context.getInput().releaseKey(HasteModClient.getActivateKey());
      context.waitTicks(1);
   }

   private static void mineUntilAir(ClientGameTestContext context, BlockPos targetPos, int maxTicks) {
      context.runOnClient(client -> HasteModClient.getActivateKey().setDown(true));
      context.getInput().holdKey(HasteModClient.getActivateKey());

      for (int i = 0; i < maxTicks; i++) {
         context.waitTick();
         context.runOnClient(client -> HasteModClient.getActivateKey().setDown(true));
         boolean isAir = (Boolean)context.computeOnClient(client -> client.level.getBlockState(targetPos).isAir());
         if (isAir) {
            break;
         }
      }

      context.runOnClient(client -> HasteModClient.getActivateKey().setDown(false));
      context.getInput().releaseKey(HasteModClient.getActivateKey());
      context.waitTicks(1);
   }

   private static void testWalkingCorridor(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
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
         singleplayer.getServer().runCommand("setblock 0 99 " + z + " bedrock");
         singleplayer.getServer().runCommand("setblock 0 102 " + z + " bedrock");
         singleplayer.getServer().runCommand("setblock -1 100 " + z + " bedrock");
         singleplayer.getServer().runCommand("setblock -1 101 " + z + " bedrock");
         singleplayer.getServer().runCommand("setblock 1 100 " + z + " bedrock");
         singleplayer.getServer().runCommand("setblock 1 101 " + z + " bedrock");
         singleplayer.getServer().runCommand("setblock 0 101 " + z + " air");
      }

      singleplayer.getServer().runCommand("setblock 0 100 0 air");

      for (int i = 0; i < rowBlocks.size(); i++) {
         int z = i + 1;
         singleplayer.getServer().runCommand("setblock 0 100 " + z + " " + rowBlocks.get(i));
      }

      singleplayer.getServer().runCommand("setblock 0 100 21 air");
      singleplayer.getServer().runCommand("setblock 0 100 22 air");
      teleportAndSettle(context, singleplayer, 0.5, 100.0, 0.5);
      context.runOnClient(client -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.CUBE;
         cfg.radius = 4;
         cfg.blocksPerTick = 10;
         cfg.tickDelay = 0;
      });
      context.getInput().pressKey(HasteModClient.getToggleKey());
      context.waitTicks(2);
      context.runOnClient(client -> {
         HasteModClient.getActivateKey().setDown(true);
         client.options.keyUp.setDown(true);
      });
      context.getInput().holdKey(HasteModClient.getActivateKey());
      context.getInput().holdKey(options -> options.keyUp);
      double currentZ = 0.0;

      for (int tick = 0; tick < 200; tick++) {
         context.waitTick();
         context.runOnClient(client -> {
            HasteModClient.getActivateKey().setDown(true);
            client.options.keyUp.setDown(true);
         });
         currentZ = (Double)context.computeOnClient(client -> client.player != null ? client.player.getZ() : 0.0);
         if (currentZ >= 20.5) {
            break;
         }
      }

      context.runOnClient(client -> {
         HasteModClient.getActivateKey().setDown(false);
         client.options.keyUp.setDown(false);
      });
      context.getInput().releaseKey(HasteModClient.getActivateKey());
      context.getInput().releaseKey(options -> options.keyUp);
      context.waitTicks(1);
      if (currentZ < 20.0) {
         throw new AssertionError("Corridor test failed: Player blocked at Z=" + currentZ);
      }

      for (int i = 0; i < rowBlocks.size(); i++) {
         BlockPos pos = new BlockPos(0, 100, i + 1);
         boolean isAir = (Boolean)context.computeOnClient(client -> client.level.getBlockState(pos).isAir());
         if (!isAir) {
            throw new AssertionError("Corridor block at " + pos + " (" + rowBlocks.get(i) + ") was NOT broken!");
         }
      }

      System.out.println("[GameTest] Stage 1 PASSED: Player traversed corridor and mined all 20 blocks.");
   }

   private static void testToolAutoSwitch(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
      System.out.println("[GameTest] --- Stage 2: Testing Tool Auto-Switching ---");
      teleportAndSettle(context, singleplayer, 0.5, 100.0, 3.5);
      context.runOnClient(client -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.CUBE;
         cfg.radius = 3;
         cfg.blocksPerTick = 1;
         cfg.tickDelay = 0;
      });
      BlockPos targetPos = new BlockPos(0, 100, 5);
      context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
      singleplayer.getServer().runCommand("setblock 0 100 5 dirt");
      waitForBlockState(context, targetPos, false);
      mineUntilAir(context, targetPos, 10);
      boolean dirtBroken = (Boolean)context.computeOnClient(client -> client.level.getBlockState(targetPos).isAir());
      int slotAfterDirt = (Integer)context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
      if (!dirtBroken) {
         throw new AssertionError("Tool auto-switch: Dirt block at " + targetPos + " was not broken!");
      }

      if (slotAfterDirt != 2) {
         throw new AssertionError("Tool auto-switch: Expected slot 2 (Shovel) for dirt, but held slot " + slotAfterDirt);
      }

      singleplayer.getServer().runCommand("setblock 0 100 5 melon");
      waitForBlockState(context, targetPos, false);
      mineUntilAir(context, targetPos, 10);
      boolean melonBroken = (Boolean)context.computeOnClient(client -> client.level.getBlockState(targetPos).isAir());
      int slotAfterMelon = (Integer)context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
      if (!melonBroken) {
         throw new AssertionError("Tool auto-switch: Melon block at " + targetPos + " was not broken!");
      }

      if (slotAfterMelon != 1) {
         throw new AssertionError("Tool auto-switch: Expected slot 1 (Axe) for melon, but held slot " + slotAfterMelon);
      }

      singleplayer.getServer().runCommand("setblock 0 100 5 stone");
      waitForBlockState(context, targetPos, false);
      mineUntilAir(context, targetPos, 10);
      boolean stoneBroken = (Boolean)context.computeOnClient(client -> client.level.getBlockState(targetPos).isAir());
      int slotAfterStone = (Integer)context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
      if (!stoneBroken) {
         throw new AssertionError("Tool auto-switch: Stone block at " + targetPos + " was not broken!");
      }

      if (slotAfterStone != 0) {
         throw new AssertionError("Tool auto-switch: Expected slot 0 (Pickaxe) for stone, but held slot " + slotAfterStone);
      }

      System.out.println("[GameTest] Stage 2 PASSED: Tool auto-switching dynamically selected Pickaxe, Axe, and Shovel.");
   }

   private static void testBlockSelectionFilter(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
      System.out.println("[GameTest] --- Stage 3: Testing Block Selection Filter Mode ---");
      teleportAndSettle(context, singleplayer, 0.5, 100.0, 3.5);
      context.runOnClient(client -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.CUBE;
         cfg.radius = 3;
         cfg.blocksPerTick = 10;
         cfg.tickDelay = 0;
      });
      context.getInput().pressKey(HasteModClient.getToggleBlockSelKey());
      context.waitTicks(2);
      BlockPos samplePos = new BlockPos(0, 100, 4);
      singleplayer.getServer().runCommand("setblock 0 100 4 stone");
      waitForBlockState(context, samplePos, false);
      context.runOnClient(client -> HasteModClient.onBlockBreak(samplePos));
      singleplayer.getServer().runCommand("setblock 0 100 4 air");
      waitForBlockState(context, samplePos, true);
      singleplayer.getServer().runCommand("setblock 0 100 5 stone");
      singleplayer.getServer().runCommand("setblock 1 100 5 stone");
      singleplayer.getServer().runCommand("setblock 0 100 6 dirt");
      singleplayer.getServer().runCommand("setblock -1 100 5 dirt");
      waitForBlockState(context, new BlockPos(0, 100, 5), false);
      mineUntilAir(context, new BlockPos(0, 100, 5), 10);
      boolean stone1Air = (Boolean)context.computeOnClient(client -> client.level.getBlockState(new BlockPos(0, 100, 5)).isAir());
      boolean stone2Air = (Boolean)context.computeOnClient(client -> client.level.getBlockState(new BlockPos(1, 100, 5)).isAir());
      if (stone1Air && stone2Air) {
         boolean dirt1Preserved = (Boolean)context.computeOnClient(client -> client.level.getBlockState(new BlockPos(0, 100, 6)).is(Blocks.DIRT));
         boolean dirt2Preserved = (Boolean)context.computeOnClient(client -> client.level.getBlockState(new BlockPos(-1, 100, 5)).is(Blocks.DIRT));
         if (dirt1Preserved && dirt2Preserved) {
            context.getInput().pressKey(HasteModClient.getToggleBlockSelKey());
            context.waitTicks(2);
            mineUntilAir(context, new BlockPos(0, 100, 6), 10);
            boolean dirt1NowAir = (Boolean)context.computeOnClient(client -> client.level.getBlockState(new BlockPos(0, 100, 6)).isAir());
            boolean dirt2NowAir = (Boolean)context.computeOnClient(client -> client.level.getBlockState(new BlockPos(-1, 100, 5)).isAir());
            if (dirt1NowAir && dirt2NowAir) {
               System.out.println("[GameTest] Stage 3 PASSED: Block selection mode selectively filtered and preserved unselected blocks.");
            } else {
               throw new AssertionError("Block selection filter: Dirt blocks were not mined after filter mode was toggled OFF!");
            }
         } else {
            throw new AssertionError("Block selection filter FAILED: Unselected dirt blocks were mined!");
         }
      } else {
         throw new AssertionError("Block selection filter: Matching stone blocks were not broken!");
      }
   }

   private static void testShapeGeometries(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
      System.out.println("[GameTest] --- Stage 4: Testing Shape Geometries (TUNNEL and LAYER) ---");
      teleportAndSettle(context, singleplayer, 0.5, 100.0, 3.5);
      context.runOnClient(client -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.TUNNEL;
         cfg.radius = 3;
         cfg.blocksPerTick = 25;
         cfg.tickDelay = 0;
      });
      singleplayer.getServer().runCommand("fill -2 100 5 2 104 5 stone");
      waitForBlockState(context, new BlockPos(0, 100, 5), false);
      mineUntilAir(context, new BlockPos(0, 100, 5), 15);

      for (int x = -1; x <= 1; x++) {
         for (int y = 100; y <= 102; y++) {
            BlockPos pos = new BlockPos(x, y, 5);
            boolean isAir = (Boolean)context.computeOnClient(client -> client.level.getBlockState(pos).isAir());
            if (!isAir) {
               throw new AssertionError("TUNNEL shape: Block inside 3x3 tunnel window at " + pos + " was NOT mined!");
            }
         }
      }

      BlockPos leftWall = new BlockPos(-2, 101, 5);
      BlockPos rightWall = new BlockPos(2, 101, 5);
      BlockPos ceiling = new BlockPos(0, 103, 5);
      boolean leftIntact = (Boolean)context.computeOnClient(client -> client.level.getBlockState(leftWall).is(Blocks.STONE));
      boolean rightIntact = (Boolean)context.computeOnClient(client -> client.level.getBlockState(rightWall).is(Blocks.STONE));
      boolean ceilIntact = (Boolean)context.computeOnClient(client -> client.level.getBlockState(ceiling).is(Blocks.STONE));
      if (leftIntact && rightIntact && ceilIntact) {
         singleplayer.getServer().runCommand("fill -2 100 5 2 104 5 air");
         context.waitTicks(2);
         teleportAndSettle(context, singleplayer, 0.5, 100.0, 3.5);
         context.runOnClient(client -> {
            HasteConfig cfg = HasteConfig.get();
            cfg.shape = Shape.LAYER;
            cfg.radius = 3;
            cfg.blocksPerTick = 10;
            cfg.tickDelay = 0;
         });
         singleplayer.getServer().runCommand("setblock 0 100 5 stone");
         singleplayer.getServer().runCommand("setblock 0 101 5 stone");
         waitForBlockState(context, new BlockPos(0, 100, 5), false);
         mineUntilAir(context, new BlockPos(0, 100, 5), 10);
         boolean layerBroken = (Boolean)context.computeOnClient(client -> client.level.getBlockState(new BlockPos(0, 100, 5)).isAir());
         boolean aboveUntouched = (Boolean)context.computeOnClient(client -> client.level.getBlockState(new BlockPos(0, 101, 5)).is(Blocks.STONE));
         if (!layerBroken) {
            throw new AssertionError("LAYER shape: Block at player level (0, 100, 5) was not broken!");
         }

         if (!aboveUntouched) {
            throw new AssertionError("LAYER shape: Block above player level (0, 101, 5) was improperly broken!");
         }

         singleplayer.getServer().runCommand("setblock 0 101 5 air");
         context.waitTicks(1);
         System.out.println("[GameTest] Stage 4 PASSED: TUNNEL and LAYER shapes respected geometric constraints.");
      } else {
         throw new AssertionError("TUNNEL shape: Blocks outside tunnel geometry were mined!");
      }
   }

   private static void testImmunityAndDisabled(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
      System.out.println("[GameTest] --- Stage 5: Testing Non-Instamineable Immunity & Disabled State ---");
      teleportAndSettle(context, singleplayer, 0.5, 100.0, 3.5);
      context.runOnClient(client -> {
         HasteConfig cfg = HasteConfig.get();
         cfg.shape = Shape.CUBE;
         cfg.radius = 3;
         cfg.blocksPerTick = 10;
         cfg.tickDelay = 0;
      });
      BlockPos targetPos = new BlockPos(0, 100, 5);
      singleplayer.getServer().runCommand("setblock 0 100 5 obsidian");
      waitForBlockState(context, targetPos, false);
      holdActivateKey(context, 5);
      boolean obsidianPreserved = (Boolean)context.computeOnClient(client -> client.level.getBlockState(targetPos).is(Blocks.OBSIDIAN));
      if (!obsidianPreserved) {
         throw new AssertionError("Non-instamineable immunity FAILED: Obsidian was broken!");
      }

      singleplayer.getServer().runCommand("setblock 0 100 5 air");
      waitForBlockState(context, targetPos, true);
      context.getInput().pressKey(HasteModClient.getToggleKey());
      context.waitTicks(2);
      singleplayer.getServer().runCommand("setblock 0 100 5 stone");
      waitForBlockState(context, targetPos, false);
      holdActivateKey(context, 5);
      boolean stoneUntouchedWhileDisabled = (Boolean)context.computeOnClient(client -> client.level.getBlockState(targetPos).is(Blocks.STONE));
      if (!stoneUntouchedWhileDisabled) {
         throw new AssertionError("Disabled state FAILED: Block was broken while HasteMod was disabled!");
      }

      context.getInput().pressKey(HasteModClient.getToggleKey());
      context.waitTicks(2);
      mineUntilAir(context, targetPos, 10);
      boolean stoneBrokenAfterReEnabled = (Boolean)context.computeOnClient(client -> client.level.getBlockState(targetPos).isAir());
      if (!stoneBrokenAfterReEnabled) {
         throw new AssertionError("Disabled state: Block was not broken after re-enabling HasteMod!");
      }

      singleplayer.getServer().runCommand("setblock 0 100 5 air");
      context.waitTicks(1);
      System.out.println("[GameTest] Stage 5 PASSED: Non-instamineable obsidian preserved; toggled-off state respected.");
   }

   private static void testTranslationsAndChat(ClientGameTestContext context) {
      System.out.println("[GameTest] --- Stage 6: Validating Translation Keys & Chat Messages ---");
      context.runOnClient(
         client -> {
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
                  throw new AssertionError("Missing translation key in Language instance: " + key);
               }

               String localized = Component.translatable(key).getString();
               if (localized.isEmpty() || localized.equals(key)) {
                  throw new AssertionError("Translation key returned raw key or empty string: " + key + " -> '" + localized + "'");
               }
            }

            try {
               Object chatComponent = getChatComponent(client);
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
                              throw new AssertionError("Found raw unlocalized key in chat message: " + text);
                           }
                        }
                     }
                  }
               }
            } catch (Throwable e) {
               if (e instanceof AssertionError ae) {
                  throw ae;
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
}
