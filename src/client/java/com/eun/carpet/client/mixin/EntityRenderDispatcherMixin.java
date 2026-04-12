package com.eun.carpet.client.mixin;

import com.eun.carpet.client.feature.ClientHighlightManager;
import com.eun.carpet.client.optimization.ClientTransparencyManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    //原有的shouldRender注入，用于隐藏实体（来自实体优化）

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onShouldRender(E entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (ClientTransparencyManager.getInstance().shouldHide(entity.getId())) {
            cir.setReturnValue(false);
        }
    }


// 在 extractEntity 返回后，修改 EntityRenderState 的 outlineColor 字段以实现高亮轮廓
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void onExtractEntity(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState state = cir.getReturnValue();
        if (state == null) return;
        if (!entity.level().isClientSide()) return;

        ClientHighlightManager mgr = ClientHighlightManager.INSTANCE;
        // 物品高亮
        if (mgr.shouldHighlightItems() && entity instanceof ItemEntity) {
            state.outlineColor = mgr.getItemColor();
            return;
        }
        // 实体高亮：生物（不包括玩家）或矿车
        if (mgr.shouldHighlightEntities()) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                state.outlineColor = mgr.getEntityColor();
            } else if (entity instanceof AbstractMinecart) {
                state.outlineColor = mgr.getEntityColor();
            }
        }
    }
}