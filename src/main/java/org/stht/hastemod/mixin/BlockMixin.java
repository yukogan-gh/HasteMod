package org.stht.hastemod.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.stht.hastemod.HasteMod;
import org.stht.hastemod.client.HasteModClient;

@Mixin(ClientPlayerInteractionManager.class)
public class BlockMixin {
    @Inject(method = "breakBlock", at = @At("HEAD"))
    public void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        HasteModClient.onBlockBreak(pos);
    }
}
