package com.eun.carpet.util;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import com.eun.carpet.EUNCarpetSettings;

/**
 * 判断一个容器显示名是否是 CCE 更新抑制器潜影盒使用的名称。
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
     * @return 是否应被视为 CCE 更新抑制器
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
