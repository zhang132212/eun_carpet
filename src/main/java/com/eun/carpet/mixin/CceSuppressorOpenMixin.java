package com.eun.carpet.mixin;

import com.eun.carpet.EUNCarpetSettings;
import com.eun.carpet.util.CceSuppressorHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

/**
 * 阻止玩家打开被用作 CCE 更新抑制器的潜影盒。
 *
 * <p>CCE 更新抑制器通过“命名潜影盒 + 比较器读取”触发，玩家一旦能打开盒子，
 * 就可以利用它搬运物品、制造部分事务，最终可能导致刷物品。这里在
 * {@link ServerPlayer#openMenu(MenuProvider)} 入口统一拦截：</p>
 *
 * <ul>
 *     <li>放置后的潜影盒方块：{@link ShulkerBoxBlockEntity}</li>
 *     <li>Quick Shulker Boxes 等数据包/模组使用的容器实体：{@link ContainerEntity}
 *     （例如 chest minecart / chest boat）</li>
 * </ul>
 *
 * <p>普通箱子、木桶等 {@code BaseContainerBlockEntity} 不受影响，避免误伤正常容器。</p>
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

        // 只拦截潜影盒方块和容器实体（chest minecart / chest boat 等）。
        // 普通箱子、木桶、熔炉等 BaseContainerBlockEntity 不在这里处理。
        if (!(provider instanceof ShulkerBoxBlockEntity) && !(provider instanceof ContainerEntity)) {
            return;
        }

        Component displayName = provider.getDisplayName();
        if (displayName == null || !CceSuppressorHelper.isSuppressorName(displayName.getString())) {
            return;
        }

        cir.setReturnValue(OptionalInt.empty());
        ((ServerPlayer) (Object) this).sendOverlayMessage(
                Component.translatable("eun_carpet.message.cce_suppressor_open_denied")
        );
    }
}
