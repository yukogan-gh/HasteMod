package org.stht.hastemod.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.stht.hastemod.client.HasteModClient;
import org.stht.hastemod.client.config.HasteConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class BlockBreaker {
    private static final Direction DEFAULT_FACE = Direction.UP;

    private Block lastMinedBlock = null;
    private boolean enabled = false;
    private boolean blockSelEnabled = false;
    private int batchCooldown = 0;

    public void onTick(Minecraft client) {
        if (client.player == null) return;

        if (HasteModClient.getToggleKey().consumeClick() && !client.isPaused()) {
            enabled = !enabled;
            client.player.displayClientMessage(Component.translatable(
                    enabled ? "msg.hastemod.toggled_on" : "msg.hastemod.toggled_off"), false);
            if (!enabled) return;
        }

        if (HasteModClient.getToggleBlockSelKey().consumeClick() && !client.isPaused()) {
            blockSelEnabled = !blockSelEnabled;
            client.player.displayClientMessage(Component.translatable(
                    blockSelEnabled ? "msg.hastemod.block_sel_toggled_on"
                                    : "msg.hastemod.block_sel_toggled_off"), false);
        }

        if (!enabled) return;
        if (blockSelEnabled && lastMinedBlock == null) return;
        if (!HasteModClient.getActivateKey().isDown()) return;

        if (batchCooldown > 0) {
            batchCooldown--;
            return;
        }

        if (client.gameMode != null && client.gameMode.isDestroying()) {
            client.gameMode.stopDestroyBlock();
        }

        HasteConfig cfg = HasteConfig.get();
        List<BlockPos> targets = collectTargets(client, cfg);
        int broken = 0;
        for (BlockPos pos : targets) {
            if (broken >= cfg.blocksPerTick) break;
            if (tryBreak(pos, client)) broken++;
        }
        if (broken > 0) batchCooldown = cfg.tickDelay;
    }

    public void onBlockBreak(BlockPos pos, Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (!blockSelEnabled) return;

        BlockState state = client.level.getBlockState(pos);
        if (updateBlock(state.getBlock())) {
            client.player.displayClientMessage(Component.translatable(
                    "msg.hastemod.selected_block", state.getBlock().getName()), false);
        }
    }

    private boolean tryBreak(BlockPos blockPos, Minecraft client) {
        if (client.level == null || client.gameMode == null || client.player == null) return false;
        BlockState state = client.level.getBlockState(blockPos);
        if (state.isAir()) return false;
        if (blockSelEnabled && !Objects.equals(state.getBlock(), lastMinedBlock)) return false;

        int currentSlot = client.player.getInventory().selected;
        int targetSlot = -1;

        if (getDestroyProgress(client, currentSlot, state, blockPos) >= 1.0F) {
            targetSlot = currentSlot;
        } else {
            float bestProgress = 0.0f;
            for (int i = 0; i < 9; i++) {
                if (i == currentSlot) continue;
                float progress = getDestroyProgress(client, i, state, blockPos);
                if (progress >= 1.0F && progress > bestProgress) {
                    targetSlot = i;
                    bestProgress = progress;
                }
            }
        }

        if (targetSlot == -1) {
            return false;
        }

        if (client.player.getInventory().selected != targetSlot) {
            int oldSlot = client.player.getInventory().selected;
            ItemStack oldItem = client.player.getInventory().getItem(oldSlot);
            ItemStack newItem = client.player.getInventory().getItem(targetSlot);

            client.player.getInventory().selected = targetSlot;

            AttributeMap attributes = client.player.getAttributes();
            if (attributes != null) {
                if (!oldItem.isEmpty()) {
                    oldItem.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
                        AttributeInstance instance = attributes.getInstance(attr);
                        if (instance != null) {
                            instance.removeModifier(mod.id());
                        }
                    });
                }
                if (!newItem.isEmpty()) {
                    newItem.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
                        AttributeInstance instance = attributes.getInstance(attr);
                        if (instance != null) {
                            instance.addOrUpdateTransientModifier(mod);
                        }
                    });
                }
            }

            if (client.getConnection() != null) {
                client.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlot));
            }
        }

        client.gameMode.startDestroyBlock(blockPos, DEFAULT_FACE);
        return true;
    }

    // progress >= 1f --> instamine
    private float getDestroyProgress(Minecraft client, int slot, BlockState state, BlockPos pos) {
        if (client.player == null || client.level == null) return 0.0f;
        ItemStack stack = client.player.getInventory().getItem(slot);
        return calculateDestroyProgress(client, stack, state, pos);
    }

    private float calculateDestroyProgress(Minecraft client, ItemStack stack, BlockState state, BlockPos pos) {
        if (client.player == null || client.level == null) return 0.0f;
        if (client.player.getAbilities().instabuild) {
            return state.getDestroySpeed(client.level, pos) < 0.0f ? 0.0f : 1.0f;
        }
        float blockHardness = state.getDestroySpeed(client.level, pos);
        if (blockHardness < 0.0f) return 0.0f;
        if (blockHardness == 0.0f) return 1.0f;

        boolean canHarvest = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state);
        int divider = canHarvest ? 30 : 100;

        float speed = stack.getDestroySpeed(state);
        if (speed > 1.0f) {
            double[] eff = new double[1];
            stack.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
                if (attr.is(Attributes.MINING_EFFICIENCY)) {
                    eff[0] += mod.amount();
                }
            });
            speed += (float) eff[0];
        }

        if (MobEffectUtil.hasDigSpeed(client.player)) {
            speed *= 1.0f + (MobEffectUtil.getDigSpeedAmplification(client.player) + 1) * 0.2f;
        }

        if (client.player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            int amp = client.player.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier();
            float f = switch (amp) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1e-4f;
            };
            speed *= f;
        }

        speed *= (float) client.player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);

        if (client.player.isEyeInFluid(FluidTags.WATER)) {
            AttributeInstance submerged = client.player.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
            if (submerged != null) {
                speed *= (float) submerged.getValue();
            }
        }

        if (!client.player.onGround()) {
            speed /= 5.0f;
        }

        return speed / blockHardness / (float) divider;
    }

    private List<BlockPos> collectTargets(Minecraft client, HasteConfig cfg) {
        assert client.player != null;
        BlockPos p = client.player.blockPosition();
        int r = cfg.radius;
        List<BlockPos> out = new ArrayList<>();
        
        java.util.function.Predicate<BlockPos> isValid = pos -> {
            if (client.level == null || client.player == null) return false;
            BlockState state = client.level.getBlockState(pos);
            if (state.isAir()) return false;
            if (blockSelEnabled && !Objects.equals(state.getBlock(), lastMinedBlock)) return false;
            double maxDistSq = client.player.isCreative() ? 36.0 : 25.0;
            if (client.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > maxDistSq) return false;
            return true;
        };

        switch (cfg.shape) {
            case CUBE -> {
                for (int x = -r; x <= r; x++)
                    for (int y = 0; y <= r; y++)
                        for (int z = -r; z <= r; z++) {
                            BlockPos pos = p.offset(x, y, z);
                            if (isValid.test(pos)) out.add(pos);
                        }
            }
            case SPHERE -> {
                int r2 = r * r;
                for (int x = -r; x <= r; x++)
                    for (int y = -r; y <= r; y++)
                        for (int z = -r; z <= r; z++)
                            if (x * x + y * y + z * z <= r2) {
                                BlockPos pos = p.offset(x, y, z);
                                if (isValid.test(pos)) out.add(pos);
                            }
            }
            case LAYER -> {
                for (int x = -r; x <= r; x++)
                    for (int z = -r; z <= r; z++) {
                        BlockPos pos = p.offset(x, 0, z);
                        if (isValid.test(pos)) out.add(pos);
                    }
            }
            case TUNNEL -> {
                Direction facing = client.player.getDirection();
                int fx = facing.getStepX();
                int fz = facing.getStepZ();
                int sx = fz;
                int sz = -fx;
                for (int forward = 1; forward <= r; forward++) {
                    for (int side = -1; side <= 1; side++) {
                        for (int dy = 0; dy <= 2; dy++) {
                            int dx = fx * forward + sx * side;
                            int dz = fz * forward + sz * side;
                            BlockPos pos = p.offset(dx, dy, dz);
                            if (isValid.test(pos)) out.add(pos);
                        }
                    }
                }
            }
        }
        out.sort(Comparator.comparingDouble(a -> a.distSqr(p)));
        return out;
    }

    private boolean updateBlock(Block block) {
        if (block == this.lastMinedBlock || !enabled) return false;
        this.lastMinedBlock = block;
        return true;
    }
}
