package com.eun.carpet.preset;

import com.eun.carpet.EUNCarpetMod;
import com.eun.carpet.config.EUNConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PresetManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EUNCarpetMod.MOD_ID + "|Preset");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = EUNConfigManager.getConfigRoot();
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("presets.json");

    @Nullable
    private static PresetConfig currentConfig = null;

    static {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            LOGGER.error("无法创建预设配置目录", e);
        }
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            PresetConfig defaultConfig = PresetConfig.createDefault();
            save(defaultConfig);
            currentConfig = defaultConfig;
            LOGGER.info("已生成默认预设配置文件: {}", CONFIG_PATH);
        } else {
            reload(null, null);
        }
    }

    public static boolean reload(@Nullable MinecraftServer server, @Nullable CommandSourceStack source) {
        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            PresetConfig newConfig = GSON.fromJson(reader, PresetConfig.class);
            if (newConfig == null) newConfig = new PresetConfig();
            if (newConfig.getPresets() == null) newConfig.setPresets(new ArrayList<>());
            Set<String> names = new HashSet<>();
            for (PresetConfig.Preset preset : newConfig.getPresets()) {
                if (preset.getName() == null || preset.getName().isEmpty()) {
                    LOGGER.warn("预设配置中存在无名称的预设，已跳过");
                } else if (!names.add(preset.getName())) {
                    LOGGER.warn("预设名称重复: {}，后面的将被忽略", preset.getName());
                }
            }
            currentConfig = newConfig;
            if (source != null) {
                // 使用局部变量避免多线程下字段变化导致的 null 警告
                PresetConfig config = currentConfig;
                int count = config != null ? config.getPresets().size() : 0;
                source.sendSuccess(() -> Component.literal("§a预设配置文件重载成功，共加载 " + count + " 个预设"), true);
            }
            return true;
        } catch (IOException | JsonParseException e) {
            LOGGER.error("重载预设配置文件失败", e);
            if (source != null) {
                source.sendFailure(Component.literal("§c预设配置文件加载失败: " + e.getMessage()));
            }
            return false;
        }
    }

    private static void save(@Nullable PresetConfig config) {
        if (config == null) return;
        try {
            Files.createDirectories(CONFIG_DIR);
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("保存预设配置文件失败", e);
        }
    }

    @Nullable
    public static PresetConfig getCurrentConfig() {
        return currentConfig;
    }

    // ✅ 按编译器建议，将 @Nullable 放在类型使用位置
    public static PresetConfig.@Nullable Preset getPreset(String name) {
        PresetConfig config = currentConfig;
        if (config == null) return null;
        for (PresetConfig.Preset preset : config.getPresets()) {
            if (preset.getName() != null && preset.getName().equals(name)) {
                return preset;
            }
        }
        return null;
    }

    public static ApplyReport applyPreset(CommandSourceStack source, String presetName) {
        ApplyReport report = new ApplyReport(presetName);
        PresetConfig config = currentConfig;
        if (config == null) {
            report.addError("配置文件未加载，请先执行 §6/eun preset reload");
            return report;
        }
        PresetConfig.Preset preset = getPreset(presetName);
        if (preset == null) {
            report.addError("预设 '" + presetName + "' 不存在");
            return report;
        }
        for (Map.Entry<String, String> entry : preset.getRules().entrySet()) {
            String ruleName = entry.getKey();
            String newValue = entry.getValue();
            String oldValue = CarpetRuleHelper.getCurrentValue(ruleName);
            if (oldValue == null) {
                report.addWarning("规则 '" + ruleName + "' 不存在，已忽略");
                continue;
            }
            if (oldValue.equals(newValue)) continue;
            boolean success = CarpetRuleHelper.setRule(source, ruleName, newValue);
            if (success) {
                report.addConflict(ruleName, newValue);
            } else {
                report.addError("规则 '" + ruleName + "' 设置失败");
            }
        }
        return report;
    }

    public static ApplyReport disablePreset(CommandSourceStack source, String presetName) {
        ApplyReport report = new ApplyReport(presetName);
        PresetConfig config = currentConfig;
        if (config == null) {
            report.addError("配置文件未加载，请先执行 §6/eun preset reload");
            return report;
        }
        PresetConfig.Preset preset = getPreset(presetName);
        if (preset == null) {
            report.addError("预设 '" + presetName + "' 不存在");
            return report;
        }
        for (String ruleName : preset.getRules().keySet()) {
            String oldValue = CarpetRuleHelper.getCurrentValue(ruleName);
            if (oldValue == null) {
                report.addWarning("规则 '" + ruleName + "' 不存在，已忽略");
                continue;
            }
            boolean success = CarpetRuleHelper.resetRuleToDefault(source, ruleName);
            if (success) {
                report.addConflict(ruleName, "默认值");
            } else {
                report.addError("规则 '" + ruleName + "' 重置失败");
            }
        }
        return report;
    }

    public static ApplyReport setDefaultPreset(CommandSourceStack source, String presetName) {
        ApplyReport report = new ApplyReport(presetName);
        PresetConfig config = currentConfig;
        if (config == null) {
            report.addError("配置文件未加载，请先执行 §6/eun preset reload");
            return report;
        }
        PresetConfig.Preset preset = getPreset(presetName);
        if (preset == null) {
            report.addError("预设 '" + presetName + "' 不存在");
            return report;
        }
        for (Map.Entry<String, String> entry : preset.getRules().entrySet()) {
            String ruleName = entry.getKey();
            String newValue = entry.getValue();
            String oldValue = CarpetRuleHelper.getCurrentValue(ruleName);
            if (oldValue == null) {
                report.addWarning("规则 '" + ruleName + "' 不存在，已忽略");
                continue;
            }
            boolean success = CarpetRuleHelper.executeSetDefault(source, ruleName, newValue);
            if (success) {
                report.addConflict(ruleName, newValue + " (默认值已设置)");
            } else {
                report.addError("规则 '" + ruleName + "' 设置默认值失败");
            }
        }
        return report;
    }

    public static class ApplyReport {
        public final String presetName;
        public final List<String> conflicts = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();

        public ApplyReport(String presetName) {
            this.presetName = presetName;
        }
        public boolean isSuccess() { return errors.isEmpty(); }
        public void addConflict(String rule, String newVal) {
            conflicts.add("§e" + rule + "§7: §f已设置为 " + newVal);
        }
        public void addWarning(String msg) {
            warnings.add("§e" + msg);
        }
        public void addError(String msg) {
            errors.add("§c" + msg);
        }
    }
    public static void reload() {
        reload(null, null);
    }
}