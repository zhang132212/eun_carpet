package com.eun.carpet.mixin;

import com.eun.carpet.EUNCarpetSettings;
import com.eun.carpet.util.CceSuppressorHelper;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 阻止 Quick Shulker 等模组的“收纳袋/捆绑包”逻辑向 CCE 抑制器潜影盒中塞入或取出物品。
 *
 * <p>这些模组会直接操作潜影盒物品的 {@code minecraft:container} 组件，不一定经过
 * {@code ServerPlayer#openMenu}，所以需要在原版 Item 的物品交互入口处提前拦截。</p>
 *
 * <p>只处理名称为 CCE 抑制器的潜影盒；未命名潜影盒和其它物品不受影响。</p>
 */
@Mixin(Item.class)
public abstract class CceSuppressorItemInteractionMixin {

    @WrapMethod(method = "overrideOtherStackedOnMe")
    private boolean eun$blockCceBundleOnMe(
            ItemStack hostStack,
            ItemStack insertStack,
            Slot slot,
            ClickAction clickType,
            Player player,
            SlotAccess cursorStackReference,
            Operation<Boolean> original
    ) {
        if (this.eun$isCceInteraction(hostStack, insertStack, player)) {
            return false;
        }
        return original.call(hostStack, insertStack, slot, clickType, player, cursorStackReference);
    }

    @WrapMethod(method = "overrideStackedOnOther")
    private boolean eun$blockCceBundleOnOther(
            ItemStack hostStack,
            Slot slot,
            ClickAction clickType,
            Player player,
            Operation<Boolean> original
    ) {
        if (this.eun$isCceInteraction(hostStack, ItemStack.EMPTY, player)) {
            return false;
        }
        return original.call(hostStack, slot, clickType, player);
    }

    @Unique
    private boolean eun$isCceInteraction(ItemStack hostStack, ItemStack insertStack, Player player) {
        if (!EUNCarpetSettings.preventCceShulkerOpen) {
            return false;
        }
        if (!CceSuppressorHelper.isSuppressorStack(hostStack)
                && !CceSuppressorHelper.isSuppressorStack(insertStack)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(
                    Component.translatable("eun_carpet.message.cce_suppressor_open_denied")
            );
            serverPlayer.containerMenu.broadcastFullState();
        }
        return true;
    }
}
