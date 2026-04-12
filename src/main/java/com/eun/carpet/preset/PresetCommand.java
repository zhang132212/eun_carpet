package com.eun.carpet.preset;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PresetCommand {

    private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS = (context, builder) -> {
        PresetConfig config = PresetManager.getCurrentConfig();
        if (config != null) {
            for (PresetConfig.Preset preset : config.getPresets()) {
                builder.suggest(preset.getName());
            }
        }
        return builder.buildFuture();
    };

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("preset")
                .then(Commands.literal("list")
                        .executes(PresetCommand::executeList))
                .then(Commands.literal("apply")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .executes(PresetCommand::executeApply)))
                .then(Commands.literal("disable")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .executes(PresetCommand::executeDisable)))
                .then(Commands.literal("setdefault")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .executes(PresetCommand::executeSetDefault)))
                .then(Commands.literal("help")
                        .executes(PresetCommand::executeHelp));
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PresetConfig config = PresetManager.getCurrentConfig();

        if (config == null) {
            source.sendFailure(Component.literal("§c预设配置文件未加载，请先执行 /eun reload"));
            return 0;
        }

        List<PresetConfig.Preset> presets = config.getPresets();
        if (presets.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e当前没有任何预设定义"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("§6=== 已加载预设列表 ==="), false);
        for (PresetConfig.Preset preset : presets) {
            String desc = preset.getDescription();
            if (desc == null || desc.isEmpty()) {
                desc = "§7无描述";
            } else {
                desc = "§f" + desc;
            }
            String finalDesc = desc;
            source.sendSuccess(() -> Component.literal("§e" + preset.getName() + " §7- " + finalDesc), false);
        }
        return 1;
    }

    private static int executeApply(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        PresetManager.ApplyReport report = PresetManager.applyPreset(source, name);
        printReport(source, report);
        return report.isSuccess() ? 1 : 0;
    }

    private static int executeDisable(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        PresetManager.ApplyReport report = PresetManager.disablePreset(source, name);
        printReport(source, report);
        return report.isSuccess() ? 1 : 0;
    }

    private static int executeSetDefault(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        PresetManager.ApplyReport report = PresetManager.setDefaultPreset(source, name);
        printReport(source, report);
        return report.isSuccess() ? 1 : 0;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6=== 预设配置文件格式说明 ==="), false);
        source.sendSuccess(() -> Component.literal("§7文件位置: §fconfig/eun_carpet/presets.json"), false);
        source.sendSuccess(() -> Component.literal("§7示例结构:"), false);
        source.sendSuccess(() -> Component.literal("§f{"), false);
        source.sendSuccess(() -> Component.literal("§f  \"presets\": ["), false);
        source.sendSuccess(() -> Component.literal("§f    {"), false);
        source.sendSuccess(() -> Component.literal("§f      \"name\": \"survival_assist\",      §7# 必填，唯一标识"), false);
        source.sendSuccess(() -> Component.literal("§f      \"description\": \"生存辅助\",     §7# 可选"), false);
        source.sendSuccess(() -> Component.literal("§f      \"rules\": {                     §7# 必填"), false);
        source.sendSuccess(() -> Component.literal("§f        \"fakePlayerPersistence\": \"true\","), false);
        source.sendSuccess(() -> Component.literal("§f        \"entityOptimizationEnabled\": \"true\""), false);
        source.sendSuccess(() -> Component.literal("§f      }"), false);
        source.sendSuccess(() -> Component.literal("§f    }"), false);
        source.sendSuccess(() -> Component.literal("§f  ]"), false);
        source.sendSuccess(() -> Component.literal("§f}"), false);
        source.sendSuccess(() -> Component.literal("§c注意事项:"), false);
        source.sendSuccess(() -> Component.literal("§7- 规则名必须为 Carpet 已知规则（含其他扩展）"), false);
        source.sendSuccess(() -> Component.literal("§7- 值一律使用字符串格式，Carpet 会自动解析"), false);
        source.sendSuccess(() -> Component.literal("§7- 布尔值必须写 §f\"true\"§7 或 §f\"false\""), false);
        source.sendSuccess(() -> Component.literal("§7- 数字写为字符串，如 §f\"10.0\""), false);
        source.sendSuccess(() -> Component.literal("§7- 修改配置后执行 §f/eun reload§7 重载"), false);
        return 1;
    }

    private static void printReport(CommandSourceStack source, PresetManager.ApplyReport report) {
        source.sendSuccess(() -> Component.literal("§6=== 预设操作报告 [" + report.presetName + "] ==="), false);
        if (report.isSuccess()) {
            source.sendSuccess(() -> Component.literal("§a✓ 操作成功"), false);
        } else {
            source.sendFailure(Component.literal("§c✗ 操作失败，存在错误"));
        }
        if (!report.conflicts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7[已生效规则]"), false);
            for (String line : report.conflicts) {
                source.sendSuccess(() -> Component.literal("  " + line), false);
            }
        }
        if (!report.warnings.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e[警告]"), false);
            for (String line : report.warnings) {
                source.sendSuccess(() -> Component.literal("  " + line), false);
            }
        }
        if (!report.errors.isEmpty()) {
            for (String line : report.errors) {
                source.sendFailure(Component.literal("  " + line));
            }
        }
    }
}