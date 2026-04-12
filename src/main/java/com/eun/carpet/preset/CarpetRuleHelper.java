package com.eun.carpet.preset;

import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Field;
import java.util.Map;

public class CarpetRuleHelper {

    @SuppressWarnings("unchecked")
    public static String getCurrentValue(String ruleName) {
        SettingsManager manager = CarpetServer.settingsManager;
        try {
            Field rulesField = SettingsManager.class.getDeclaredField("rules");
            rulesField.setAccessible(true);
            Map<String, Object> rules = (Map<String, Object>) rulesField.get(manager);
            Object rule = rules.get(ruleName);
            if (rule == null) return null;
            return String.valueOf(rule);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean setRule(CommandSourceStack source, String ruleName, String value) {
        String command = "carpet setDefault " + ruleName + " " + value;
        return executeCommand(source, command);
    }

    public static boolean resetRuleToDefault(CommandSourceStack source, String ruleName) {
        String command = "carpet removeDefault " + ruleName;
        return executeCommand(source, command);
    }

    public static boolean executeSetDefault(CommandSourceStack source, String ruleName, String value) {
        String command = "carpet setDefault " + ruleName + " " + value;
        return executeCommand(source, command);
    }

    private static boolean executeCommand(CommandSourceStack source, String command) {
        try {
            source.getServer().getCommands().performPrefixedCommand(source, command);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}