package org.stht.client;

import net.minecraft.client.Minecraft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class BlockBreaker {
    private final Direction direction = Direction.UP;
    private Block lastMinedBlock = null;
    private boolean enabled = false;
    private boolean blockSelEnabled = false;

    public void tryBreak(BlockPos blockPos, Minecraft client) {
        if (client.level == null || client.gameMode == null || client.player == null) {
            return;
        }
        BlockState state = client.level.getBlockState(blockPos);
        if (!state.isAir() && (!blockSelEnabled || Objects.equals(client.level.getBlockState(blockPos).getBlock(), lastMinedBlock))) {
            float speed = getMineSpeed(client, client.player.getInventory().getSelectedSlot(), state);
            if (speed <= 1.0f) {
                for (int i = 0; i < client.player.getInventory().getNonEquipmentItems().size(); i++) {

                    float newSpeed = getMineSpeed(client, i, state);

                    if (newSpeed > 1.0f) {
                        if (client.player.getInventory().getSelectedSlot() == i) break;
                        client.gameMode.handleContainerInput(client.player.containerMenu.containerId, 36 + client.player.getInventory().getSelectedSlot(), i, ContainerInput.SWAP, client.player);
                        break;
                    }
                }
            }
            client.gameMode.startDestroyBlock(blockPos, direction);
            //client.interactionManager.updateBlockBreakingProgress(blockPos, this.direction);
        }
    }

    private float getMineSpeed(Minecraft client, int slot, BlockState blockState) {
        if (client.player == null) return 1.0f;
        return client.player.getInventory().getNonEquipmentItems().get(slot).getDestroySpeed(blockState);
    }

    public void onTick(Minecraft client) {
        if (client.player == null) return;
        if (HasteModClient.getToggleKey().consumeClick() && !client.isPaused()) {
            enabled = !enabled;
            if (enabled) {
                client.player.sendSystemMessage(Component.nullToEmpty("§eToggled HasteMod to §aon"));
            } else {
                client.player.sendSystemMessage(Component.nullToEmpty("§eToggled HasteMod to §coff"));
                return;
            }
        }
        if (HasteModClient.getToggleBlockSelKey().consumeClick() && !client.isPaused()) {
            blockSelEnabled = !blockSelEnabled;
            if (blockSelEnabled) {
                client.player.sendSystemMessage(Component.nullToEmpty("§eToggled §dBlock Selection Mode §eto §aon"));
            } else {
                client.player.sendSystemMessage(Component.nullToEmpty("§eToggled §dBlock Selection Mode §eto §coff"));
            }
        }
        if ((lastMinedBlock == null && blockSelEnabled) || !enabled ) return;
        if (!HasteModClient.getActivateKey().isDown()) return;

        int range = 4;
        for (int x = client.player.blockPosition().getX() - range; x <= client.player.blockPosition().getX() + range; x++) {
            for (int y = client.player.blockPosition().getY(); y <= client.player.blockPosition().getY() + range; y++) {
                for (int z = client.player.blockPosition().getZ() - range; z <= client.player.blockPosition().getZ() + range; z++) {
                    BlockPos nbp = new BlockPos(x, y, z);
                    tryBreak(nbp, client);
                }
            }
        }
    }

    public void onBlockBreak(BlockPos pos, Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (client.player.getUUID() != Minecraft.getInstance().player.getUUID()) return;
        if (!blockSelEnabled) return;

        BlockState state = client.level.getBlockState(pos);

        if (updateBlock(state.getBlock())) {
            client.player.sendSystemMessage(Component.nullToEmpty("§eSelected block: §3" + state.getBlock().getName().getString()));
        }
    }

    public boolean updateBlock(Block block) {
        if (block == this.lastMinedBlock || !enabled) return false;
        this.lastMinedBlock = block;
        return true;
    }
}
