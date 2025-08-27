package org.stht.hastemod.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

public class BlockBreaker {
    private final Direction direction = Direction.UP;
    private Block lastMinedBlock = null;
    private boolean enabled = false;
    private boolean blockSelEnabled = false;

    @Environment(EnvType.CLIENT)
    public void tryBreak(BlockPos blockPos, MinecraftClient client) {
        if (client.world == null || client.interactionManager == null || client.player == null) {
            return;
        }
        BlockState state = client.world.getBlockState(blockPos);
        if (!state.isAir() && (!blockSelEnabled || Objects.equals(client.world.getBlockState(blockPos).getBlock(), lastMinedBlock))) {
            float speed = getMineSpeed(client, client.player.getInventory().getSelectedSlot(), state);
            if (speed <= 1.0f) {
                for (int i = 0; i < client.player.getInventory().getMainStacks().size(); i++) {

                    float newSpeed = getMineSpeed(client, i, state);

                    if (newSpeed > 1.0f) {
                        if (client.player.getInventory().getSelectedSlot() == i) break;
                        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 36 + client.player.getInventory().getSelectedSlot(), i, SlotActionType.SWAP, client.player);
                        break;
                    }
                }
            }
            client.interactionManager.attackBlock(blockPos, direction);
            //client.interactionManager.updateBlockBreakingProgress(blockPos, this.direction);
        }
    }

    private float getMineSpeed(MinecraftClient client, int slot, BlockState blockState) {
        if (client.player == null) return 1.0f;
        return client.player.getInventory().getMainStacks().get(slot).getMiningSpeedMultiplier(blockState);
    }

    public void onTick(MinecraftClient client) {
        if (client.player == null) return;
        if (HasteModClient.getToggleKey().wasPressed() && !client.isPaused()) {
            enabled = !enabled;
            if (enabled) {
                client.player.sendMessage(Text.of("§eToggled HasteMod to §aon"), false);
            } else {
                client.player.sendMessage(Text.of("§eToggled HasteMod to §coff"), false);
                return;
            }
        }
        if (HasteModClient.getToggleBlockSelKey().wasPressed() && !client.isPaused()) {
            blockSelEnabled = !blockSelEnabled;
            if (blockSelEnabled) {
                client.player.sendMessage(Text.of("§eToggled §dBlock Selection Mode §eto §aon"), false);
            } else {
                client.player.sendMessage(Text.of("§eToggled §dBlock Selection Mode §eto §coff"), false);
            }
        }
        if ((lastMinedBlock == null && blockSelEnabled) || !enabled ) return;
        if (!HasteModClient.getActivateKey().isPressed()) return;

        int range = 4;
        for (int x = client.player.getBlockPos().getX() - range; x <= client.player.getBlockPos().getX() + range; x++) {
            for (int y = client.player.getBlockPos().getY(); y <= client.player.getBlockPos().getY() + range; y++) {
                for (int z = client.player.getBlockPos().getZ() - range; z <= client.player.getBlockPos().getZ() + range; z++) {
                    BlockPos nbp = new BlockPos(x, y, z);
                    tryBreak(nbp, client);
                }
            }
        }
    }

    public void onBlockBreak(BlockPos pos, MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (client.player.getUuid() != MinecraftClient.getInstance().player.getUuid()) return;
        if (!blockSelEnabled) return;

        BlockState state = client.world.getBlockState(pos);

        if (updateBlock(state.getBlock())) {
            client.player.sendMessage(Text.of("§eSelected block: §3" + state.getBlock().getName().getString()), false);
        }
    }

    public boolean updateBlock(Block block) {
        if (block == this.lastMinedBlock || !enabled) return false;
        this.lastMinedBlock = block;
        return true;
    }
}
