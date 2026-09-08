package com.eun.carpet.client.mixin;

import com.eun.carpet.EUNCarpetSettings;
import com.eun.carpet.util.CceSuppressorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端拦截：光标拿着 CCE 抑制器潜影盒时，右键点击槽位不再触发 Quick Shulker 的拖拽装盒。
 *
 * <p>Quick Shulker 的 {@code MouseDraggedHandler} 会直接调用 {@code slotClicked}，
 * 在这里提前取消，客户端不会发出装盒点击包。</p>
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ClientCceSuppressorSlotClickMixin {

    @Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eun$blockCceBundleClick(
            Slot slot,
            int slotId,
            int button,
            ContainerInput input,
            CallbackInfo ci
    ) {
        if (!EUNCarpetSettings.preventCceShulkerOpen || input != ContainerInput.PICKUP || button != 1) {
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (!CceSuppressorHelper.isSuppressorStack(screen.getMenu().getCarried())) {
            return;
        }
        ci.cancel();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendOverlayMessage(
                    Component.translatable("eun_carpet.message.cce_suppressor_open_denied")
            );
        }
    }
}
