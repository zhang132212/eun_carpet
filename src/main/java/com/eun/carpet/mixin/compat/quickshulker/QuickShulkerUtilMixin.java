package com.eun.carpet.mixin.compat.quickshulker;

import com.eun.carpet.util.CceSuppressorHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quick Shulker 兼容层：在它真正调用 openMenu 之前就拦下 CCE 抑制器潜影盒。
 *
 * <p>Quick Shulker 的 {@code Util.openItem} 在 openMenu 被取消后仍会继续执行，
 * 把“已使用槽位”和关闭监听器挂到当前菜单上，导致背包界面被关闭、光标残留物品等
 * 客户端/服务端状态错乱。因此在 Quick Shulker 自己的入口处直接取消，
 * 让它对 CCE 抑制器潜影盒完全不产生副作用。</p>
 *
 * <p>该 mixin 放在独立的 optional 配置里；未安装 Quick Shulker 时不会加载。</p>
 */
@Mixin(targets = "net.kyrptonaught.quickshulker.api.Util", remap = false)
public abstract class QuickShulkerUtilMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|QuickShulker");

    @Inject(
            method = "openItem(Lnet/minecraft/world/entity/player/Player;I)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void eun$blockCceOpenByMenuSlot(Player player, int menuSlot, CallbackInfo ci) {
        if (player == null || menuSlot < 0 || menuSlot >= player.containerMenu.slots.size()) {
            return;
        }
        Slot slot = player.containerMenu.slots.get(menuSlot);
        if (slot != null && CceSuppressorHelper.isSuppressorStack(slot.getItem())) {
            ci.cancel();
            notifyPlayer(player);
        }
    }

    @Inject(
            method = "openItem(Lnet/minecraft/world/entity/player/Player;II)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void eun$blockCceOpenByInvSlot(Player player, int menuSlot, int playerInvSlot, CallbackInfo ci) {
        if (player == null || playerInvSlot < 0 || playerInvSlot >= player.getInventory().getContainerSize()) {
            return;
        }
        ItemStack stack = player.getInventory().getItem(playerInvSlot);
        if (CceSuppressorHelper.isSuppressorStack(stack)) {
            ci.cancel();
            notifyPlayer(player);
        }
    }

    private static void notifyPlayer(Player player) {
        LOGGER.info("[EUNCarpet] Blocked Quick Shulker CCE open for {}", player.getName().getString());
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("eun_carpet.message.cce_suppressor_open_denied")
            );
        }
    }
}
