package com.eun.carpet.mixin;

import com.eun.carpet.EUNCarpetSettings;
import com.eun.carpet.util.CceSuppressorHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务端兜底：阻止“光标拿着 CCE 抑制器潜影盒，右键点击其它槽位”的收纳袋式装盒。
 *
 * <p>Quick Shulker 的拖拽装盒最终会走 {@code AbstractContainerMenu#clicked}，
 * 在服务端再拦一层，避免恶意客户端绕过客户端限制。</p>
 */
@Mixin(AbstractContainerMenu.class)
public abstract class CceSuppressorContainerClickMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|CCE");

    @Inject(
            method = "clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eun$blockCceBundleClick(
            int slotId,
            int button,
            ContainerInput input,
            Player player,
            CallbackInfo ci
    ) {
        if (input != ContainerInput.PICKUP || button != 1) {
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (!EUNCarpetSettings.preventCceShulkerOpen || !CceSuppressorHelper.isSuppressorStack(menu.getCarried())) {
            return;
        }
        ci.cancel();
        LOGGER.info("[EUNCarpet] Blocked CCE bundle click: player={}, slot={}, button={}, input={}", player.getName().getString(), slotId, button, input);
        // 取消后强制同步一次菜单，避免客户端预测的装盒状态残留
        menu.broadcastFullState();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("eun_carpet.message.cce_suppressor_open_denied")
            );
        }
    }
}
