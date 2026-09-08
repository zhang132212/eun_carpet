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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Quick Shulker 兼容层：拦截它所有“收纳袋式”装盒/取盒入口。
 *
 * <p>Quick Shulker 除了普通容器点击，还有自己的 {@code QuickBundlePacket} 等自定义包，
 * 最终都会调用 {@code BundleHelper} 的静态方法。之前只拦 Item/容器点击，
 * 创造模式下的自定义包路径会绕过它们，所以这里直接在 BundleHelper 入口拦截。</p>
 *
 * <p>该 mixin 放在独立的 optional 配置里；未安装 Quick Shulker 时不会加载。</p>
 */
@Mixin(targets = "net.kyrptonaught.quickshulker.util.BundleHelper", remap = false)
public abstract class QuickShulkerBundleHelperMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|QuickShulker");

    @Inject(
            method = "bundleItemIntoStack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void eun$blockBundleIntoStack(
            Player player,
            ItemStack hostStack,
            ItemStack insertStack,
            CallbackInfoReturnable<Boolean> cir,
            CallbackInfo ci
    ) {
        if (isCce(hostStack) || isCce(insertStack)) {
            ci.cancel();
            deny(player, hostStack, insertStack, "bundleItemIntoStack");
        }
    }

    @Inject(
            method = "bundleItemIntoStack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void eun$blockBundleIntoStackSlot(
            Player player,
            ItemStack hostStack,
            ItemStack insertStack,
            Slot slot,
            CallbackInfoReturnable<Boolean> cir,
            CallbackInfo ci
    ) {
        if (isCce(hostStack) || isCce(insertStack)) {
            ci.cancel();
            deny(player, hostStack, insertStack, "bundleItemIntoStack(slot)");
        }
    }

    @Inject(
            method = "unbundleStackIntoSlot(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void eun$blockUnbundle(
            Player player,
            ItemStack hostStack,
            Slot slot,
            CallbackInfoReturnable<Boolean> cir,
            CallbackInfo ci
    ) {
        if (isCce(hostStack)) {
            ci.cancel();
            deny(player, hostStack, ItemStack.EMPTY, "unbundleStackIntoSlot");
        }
    }

    @Inject(
            method = "unbundleItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void eun$blockUnbundleItem(
            Player player,
            ItemStack hostStack,
            Slot slot,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (isCce(hostStack)) {
            cir.setReturnValue(ItemStack.EMPTY);
            deny(player, hostStack, ItemStack.EMPTY, "unbundleItem");
        }
    }

    @Inject(
            method = "transferItemsToShulker(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void eun$blockTransfer(
            Player player,
            ItemStack hostStack,
            ItemStack insertStack,
            CallbackInfoReturnable<Boolean> cir,
            CallbackInfo ci
    ) {
        if (isCce(hostStack) || isCce(insertStack)) {
            ci.cancel();
            deny(player, hostStack, insertStack, "transferItemsToShulker");
        }
    }

    private static boolean isCce(ItemStack stack) {
        return CceSuppressorHelper.isSuppressorStack(stack);
    }

    private static void deny(Player player, ItemStack hostStack, ItemStack insertStack, String method) {
        LOGGER.info("[EUNCarpet] Blocked Quick Shulker {} for CCE box: player={}, host={}, insert={}",
                method,
                player == null ? "?" : player.getName().getString(),
                hostStack == null ? "?" : hostStack.getHoverName().getString(),
                insertStack == null ? "?" : insertStack.getHoverName().getString());
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("eun_carpet.message.cce_suppressor_open_denied")
            );
            serverPlayer.containerMenu.broadcastFullState();
        }
    }
}
