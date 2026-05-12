package org.stht.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.stht.client.HasteModClient;

@Mixin(MultiPlayerGameMode.class)
public class BlockMixin {
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    public void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        HasteModClient.onBlockBreak(pos);
    }
}
