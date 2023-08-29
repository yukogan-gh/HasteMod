package org.stht.hastemod.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

public class BlockBreaker {
    private final Direction direction = Direction.UP;
    private Block lastMinedBlock = null;
    private boolean enabled = false;
    private final int range = 4;

    @Environment(EnvType.CLIENT)
    public void tryBreak(BlockPos blockPos, MinecraftClient client) {
        if (!client.world.getBlockState(blockPos).isAir() && Objects.equals(client.world.getBlockState(blockPos).getBlock(), lastMinedBlock)) {
            client.interactionManager.attackBlock(blockPos, direction);
            client.interactionManager.updateBlockBreakingProgress(blockPos, this.direction);
        }
    }

    public void onTick(MinecraftClient client) {
        if (client.player == null) return;
        if (HasteModClient.getToggleKey().wasPressed() && !client.isPaused()) {
            enabled = !enabled;
            if (enabled) {
                client.player.sendMessage(Text.of("§eToggled HasteMod to §aon"));
            } else {
                client.player.sendMessage(Text.of("§eToggled HasteMod to §coff"));
                return;
            }
        }
        if (lastMinedBlock == null || !enabled) return;
        if (HasteModClient.getActivateKey().isPressed()) {
            for (int x = client.player.getBlockPos().getX() - range; x < client.player.getBlockPos().getX() + range; x++) {
                for (int y = client.player.getBlockPos().getY(); y < client.player.getBlockPos().getY() + range; y++) {
                    for (int z = client.player.getBlockPos().getZ() - range; z < client.player.getBlockPos().getZ() + range; z++) {
                        BlockPos nbp = new BlockPos(x, y, z);
                        tryBreak(nbp, client);
                    }
                }
            }
        }

    }

    public boolean onBlockBreak(BlockPos pos, MinecraftClient client) {
        if (client.player == null) return true;
        if (client.player.getUuid() != MinecraftClient.getInstance().player.getUuid()) return true;
        BlockState state = client.world.getBlockState(pos);

        if (updateBlock(state.getBlock())) {
            client.player.sendMessage(Text.of("§eSelected block: §3" + state.getBlock().getName().getString()));
        }
        return true;
    }

    public boolean updateBlock(Block block) {
        if (block == this.lastMinedBlock || !enabled) return false;
        this.lastMinedBlock = block;
        return true;
    }
}
