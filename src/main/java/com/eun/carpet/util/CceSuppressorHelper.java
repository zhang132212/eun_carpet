package com.eun.carpet.util;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import com.eun.carpet.EUNCarpetSettings;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * 判断一个容器/物品是否属于 CCE 更新抑制器潜影盒。
 *
 * <p>如果服务端安装了 Carpet Org Addition，则优先读取它的
 * {@code CCEUpdateSuppression} 规则，并保持与 Carpet Org Addition 完全一致
 * 的名称匹配逻辑；如果没有安装，则回退到 eun_carpet 的
 * {@code cceSuppressorNames} 规则（逗号分隔）。</p>
 */
public final class CceSuppressorHelper {
    private static final String CCE_RULE_NAME = "CCEUpdateSuppression";

    private CceSuppressorHelper() {
    }

    /**
     * @param displayName 容器/潜影盒的显示名
     * @return 是否应被视为 CCE 更新抑制器名称
     */
    public static boolean isSuppressorName(String displayName) {
        if (displayName == null || !EUNCarpetSettings.preventCceShulkerOpen) {
            return false;
        }

        String configured = getCarpetOrgAdditionRuleValue();
        if (configured != null) {
            return matchesCarpetOrgAdditionRule(configured, displayName);
        }

        return matchesFallbackNames(displayName);
    }

    /**
     * 判断一个物品是否是“带有更新抑制器名称的任意颜色潜影盒”。
     *
     * <p>只有已命名的潜影盒才会命中；未命名的普通潜影盒不会受到影响。</p>
     *
     * @param stack 待检查物品
     * @return 是否为 CCE 更新抑制器潜影盒
     */
    public static boolean isSuppressorStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
            return false;
        }
        return isSuppressorName(stack.getHoverName().getString());
    }

    /**
     * 在玩家物品栏中查找与给定显示名匹配的 CCE 抑制器潜影盒。
     *
     * <p>Quick Shulker 等模组打开的是物品栏里的潜影盒物品，而不是方块实体，
     * 所以需要在玩家物品栏中做一次精确匹配，避免误伤其它未命名潜影盒。</p>
     */
    public static boolean inventoryContainsSuppressorWithName(Player player, String displayName) {
        if (player == null || displayName == null) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isSuppressorStack(stack) && displayName.equals(stack.getHoverName().getString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取 Carpet Org Addition 的 CCEUpdateSuppression 规则值。
     *
     * @return 规则值；规则不存在或读取失败时返回 {@code null}
     */
    private static String getCarpetOrgAdditionRuleValue() {
        try {
            if (CarpetServer.settingsManager == null) {
                return null;
            }
            CarpetRule<?> rule = CarpetServer.settingsManager.getCarpetRule(CCE_RULE_NAME);
            if (rule == null) {
                return null;
            }
            Object value = rule.value();
            return value == null ? null : String.valueOf(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * 与 Carpet Org Addition 的 RuleUtils#canUpdateSuppression 保持一致。
     */
    private static boolean matchesCarpetOrgAdditionRule(String configured, String displayName) {
        if ("false".equals(configured)) {
            return false;
        }
        if ("true".equals(configured)) {
            return "更新抑制器".equals(displayName) || "updateSuppression".equalsIgnoreCase(displayName);
        }
        return configured.equalsIgnoreCase(displayName);
    }

    /**
     * 未安装 Carpet Org Addition 时使用的回退名称列表。
     */
    private static boolean matchesFallbackNames(String displayName) {
        String names = EUNCarpetSettings.cceSuppressorNames;
        if (names == null || names.isEmpty()) {
            return false;
        }
        for (String candidate : names.split(",")) {
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty() && trimmed.equalsIgnoreCase(displayName)) {
                return true;
            }
        }
        return false;
    }
}
