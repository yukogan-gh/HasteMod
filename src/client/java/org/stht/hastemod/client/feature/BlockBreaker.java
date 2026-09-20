package org.stht.hastemod.client.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.stht.hastemod.client.HasteModClient;
import org.stht.hastemod.client.config.HasteConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Environment(EnvType.CLIENT)
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
            client.player.sendSystemMessage(Component.translatable(
                    enabled ? "msg.hastemod.toggled_on" : "msg.hastemod.toggled_off"));
            if (!enabled) return;
        }

        if (HasteModClient.getToggleBlockSelKey().consumeClick() && !client.isPaused()) {
            blockSelEnabled = !blockSelEnabled;
            client.player.sendSystemMessage(Component.translatable(
                    blockSelEnabled ? "msg.hastemod.block_sel_toggled_on"
                                    : "msg.hastemod.block_sel_toggled_off"));
        }

        if (!enabled) return;
        if (blockSelEnabled && lastMinedBlock == null) return;
        if (!HasteModClient.getActivateKey().isDown()) return;

        if (batchCooldown > 0) {
            batchCooldown--;
            return;
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
            client.player.sendSystemMessage(Component.translatable(
                    "msg.hastemod.selected_block", state.getBlock().getName()));
        }
    }

    private boolean tryBreak(BlockPos blockPos, Minecraft client) {
        if (client.level == null || client.gameMode == null || client.player == null) return false;
        BlockState state = client.level.getBlockState(blockPos);
        if (state.isAir()) return false;
        if (blockSelEnabled && !Objects.equals(state.getBlock(), lastMinedBlock)) return false;

        float speed = getMineSpeed(client, client.player.getInventory().getSelectedSlot(), state);
        if (speed <= 1.0f) {
            for (int i = 0; i < 9; i++) {
                float newSpeed = getMineSpeed(client, i, state);
                if (newSpeed > 1.0f) {
                    if (client.player.getInventory().getSelectedSlot() != i) {
                        client.player.getInventory().setSelectedSlot(i);
                    }
                    break;
                }
            }
        }
        client.gameMode.startDestroyBlock(blockPos, DEFAULT_FACE);
        return true;
    }

    private float getMineSpeed(Minecraft client, int slot, BlockState state) {
        if (client.player == null) return 1.0f;
        return client.player.getInventory().getNonEquipmentItems().get(slot).getDestroySpeed(state);
    }

    private List<BlockPos> collectTargets(Minecraft client, HasteConfig cfg) {
        assert client.player != null;
        BlockPos p = client.player.blockPosition();
        int r = cfg.radius;
        List<BlockPos> out = new ArrayList<>();
        
        java.util.function.Predicate<BlockPos> isValid = pos -> {
            if (client.level == null) return false;
            BlockState state = client.level.getBlockState(pos);
            if (state.isAir()) return false;
            if (blockSelEnabled && !Objects.equals(state.getBlock(), lastMinedBlock)) return false;
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
