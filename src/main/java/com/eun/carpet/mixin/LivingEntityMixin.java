package com.eun.carpet.mixin;

import com.eun.carpet.aioptimization.AIOptimizedEntity;
import com.eun.carpet.aioptimization.AIOptimizedGroup;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements AIOptimizedEntity {
    @Unique private boolean eun$optimized = false;
    @Unique private AIOptimizedGroup eun$group;

    @Override
    public boolean eun$isOptimized() { return eun$optimized; }
    @Override
    public void eun$setOptimized(boolean opt) { eun$optimized = opt; }
    @Override
    public AIOptimizedGroup eun$getGroup() { return eun$group; }
    @Override
    public void eun$setGroup(AIOptimizedGroup group) { eun$group = group; }

    // 禁用移动
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(CallbackInfo ci) {
        if (eun$optimized) {
            ci.cancel();
        }
    }

    // 禁用跳跃
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void onJump(CallbackInfo ci) {
        if (eun$optimized) {
            ci.cancel();
        }
    }
}