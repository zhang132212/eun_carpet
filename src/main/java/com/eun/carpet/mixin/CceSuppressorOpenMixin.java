package com.eun.carpet.mixin;

import com.eun.carpet.EUNCarpetSettings;
import com.eun.carpet.util.CceSuppressorHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

/**
 * 阻止玩家打开“带有更新抑制器名称的任意颜色潜影盒”。
 *
 * <p>只拦截 CCE 抑制器潜影盒，不会影响未命名的普通潜影盒。覆盖以下路径：</p>
 *
 * <ul>
 *     <li>放置后的潜影盒方块：{@link ShulkerBoxBlockEntity}</li>
 *     <li>Quick Shulker Boxes 数据包使用的容器实体：{@link ContainerEntity}
 *     （例如 chest minecart / chest boat）</li>
 *     <li>Quick Shulker 模组等使用 {@link SimpleMenuProvider} 打开物品栏内潜影盒物品的路径</li>
 * </ul>
 */
@Mixin(ServerPlayer.class)
public abstract class CceSuppressorOpenMixin {

    @Inject(
            method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eun$preventOpeningCceSuppressor(
            MenuProvider provider,
            CallbackInfoReturnable<OptionalInt> cir
    ) {
        if (!EUNCarpetSettings.preventCceShulkerOpen || provider == null) {
            return;
        }

        ServerPlayer player = (ServerPlayer) (Object) this;

        // 1. 放置后的潜影盒方块 / 容器实体（Quick Shulker Boxes 数据包等）。
        if (provider instanceof ShulkerBoxBlockEntity || provider instanceof ContainerEntity) {
            // 未命名的普通潜影盒/容器不拦截。
            if (provider instanceof Nameable nameable && !nameable.hasCustomName()) {
                return;
            }
            Component displayName = provider.getDisplayName();
            if (displayName == null || !CceSuppressorHelper.isSuppressorName(displayName.getString())) {
                return;
            }
            this.eun$deny(player, cir);
            return;
        }

        // 2. Quick Shulker 模组：使用 SimpleMenuProvider 打开物品栏里的潜影盒物品。
        //    它不经过方块实体，也不经过 ContainerEntity，需要按“物品栏中是否有同名 CCE 盒”判断。
        if (provider instanceof SimpleMenuProvider) {
            Component displayName = provider.getDisplayName();
            if (displayName == null || !CceSuppressorHelper.isSuppressorName(displayName.getString())) {
                return;
            }
            if (!CceSuppressorHelper.inventoryContainsSuppressorWithName(player, displayName.getString())) {
                return;
            }
            this.eun$deny(player, cir);
        }
    }

    @Unique
    private void eun$deny(ServerPlayer player, CallbackInfoReturnable<OptionalInt> cir) {
        cir.setReturnValue(OptionalInt.empty());
        player.sendOverlayMessage(Component.translatable("eun_carpet.message.cce_suppressor_open_denied"));
    }
}
