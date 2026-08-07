package com.eun.carpet.command;

import com.eun.carpet.EUNCarpetSettings;
import com.eun.carpet.commandgui.ServerCommandConfigManager;
import com.eun.carpet.config.EUNConfigManager;
import com.eun.carpet.highlight.HighlightManager;
import com.eun.carpet.pearlcannon.PearlCannonManager;
import com.eun.carpet.pearlcannon.PearlCannonScheme;
import com.eun.carpet.packet.PacketCommand;
import com.eun.carpet.preset.PresetCommand;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;

import java.util.List;
import java.util.Map;

public class EUNCommands {
    private static final SimpleCommandExceptionType NO_SUCH_SCHEME = new SimpleCommandExceptionType(Component.literal("不存在的方案名"));
    private static final Map<String, Integer> HIGHLIGHT_COLORS = Map.ofEntries(
            Map.entry("black", 0x000000),
            Map.entry("dark_blue", 0x0000AA),
            Map.entry("dark_green", 0x00AA00),
            Map.entry("dark_aqua", 0x00AAAA),
            Map.entry("dark_red", 0xAA0000),
            Map.entry("dark_purple", 0xAA00AA),
            Map.entry("gold", 0xFFAA00),
            Map.entry("gray", 0xAAAAAA),
            Map.entry("dark_gray", 0x555555),
            Map.entry("blue", 0x5555FF),
            Map.entry("green", 0x55FF55),
            Map.entry("aqua", 0x55FFFF),
            Map.entry("red", 0xFF5555),
            Map.entry("light_purple", 0xFF55FF),
            Map.entry("yellow", 0xFFFF55),
            Map.entry("white", 0xFFFFFF)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.getRoot().getChildren().removeIf(node -> node.getName().equals("eun"));

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("eun")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("可用子命令: /eun reload" +
                            (EUNCarpetSettings.highlightEnabled.equals("false") ? "" : ", /eun highlight") +
                            (EUNCarpetSettings.pearlCannonEnabled.equals("false") ? "" : ", /eun pearlCannon") +
                            (EUNCarpetSettings.packetEnabled.equals("false") ? "" : ", /eun packet")), false);
                    return 1;
                });

        // 统一重载命令 - 始终注册，无权限限制
        root.then(Commands.literal("reload")
                .executes(EUNCommands::executeReloadAll));

        // 以下子命令根据当前规则动态注册
        if (!EUNCarpetSettings.highlightEnabled.equals("false")) {
            root.then(createHighlightCommand());
        }
        if (!EUNCarpetSettings.pearlCannonEnabled.equals("false")) {
            root.then(createPearlCannonCommand());
        }
        if (!EUNCarpetSettings.packetEnabled.equals("false")) {
            root.then(PacketCommand.build());
        }
        if (!EUNCarpetSettings.presetEnabled.equals("false")) {
            root.then(PresetCommand.build());
        }

        dispatcher.register(root);

    }

    private static int executeReloadAll(CommandContext<CommandSourceStack> ctx) {
        EUNConfigManager.reloadAllConfigs();
        // /eun reload 执行后立即重载配置文件并推送给所有在线玩家
        ServerCommandConfigManager.reloadAndPush(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("所有 EUN 配置文件已重新加载"), true);
        return 1;
    }

    // 权限检查方法（供其他子命令使用）
    private static boolean checkPermission(CommandSourceStack source, int requiredLevel) {
        if (source.getEntity() == null) return true; // 控制台
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            GameProfile profile = player.getGameProfile();
            NameAndId nameAndId = new NameAndId(profile.id(), profile.name());
            var ops = source.getServer().getPlayerList().getOps();
            ServerOpListEntry entry = ops.get(nameAndId);
            if (entry != null) {
                return entry.permissions().level().id() >= requiredLevel;
            }
            return requiredLevel == 0;
        }
        return false;
    }

    private static boolean checkPermission(CommandSourceStack source, String ruleValue) {
        if (ruleValue.equals("true")) return true;
        if (ruleValue.equals("false")) return false;
        try {
            int level = Integer.parseInt(ruleValue);
            return checkPermission(source, level);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createHighlightCommand() {
        String ruleValue = EUNCarpetSettings.highlightEnabled;
        return Commands.literal("highlight")
                .requires(source -> checkPermission(source, ruleValue))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal("用法: /eun highlight <item|entity> <true|false>"));
                    return 0;
                })
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("item");
                            builder.suggest("entity");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    if (EUNCarpetSettings.highlightEnabled.equals("false")) {
                                        ctx.getSource().sendFailure(Component.literal("高亮功能已禁用"));
                                        return 0;
                                    }
                                    String type = StringArgumentType.getString(ctx, "type");
                                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");

                                    HighlightManager mgr = HighlightManager.getInstance();
                                    boolean items = mgr.isHighlightItems();
                                    boolean entities = mgr.isHighlightEntities();
                                    if (type.equalsIgnoreCase("item")) {
                                        items = enabled;
                                    } else if (type.equalsIgnoreCase("entity")) {
                                        entities = enabled;
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("类型必须是 item 或 entity"));
                                        return 0;
                                    }
                                    mgr.setGlobal(items, entities);

                                    final boolean fItems = items;
                                    final boolean fEntities = entities;
                                    ctx.getSource().sendSuccess(() -> Component.literal("全局高亮已设置: item=" + fItems + " entity=" + fEntities), true);
                                    return 1;
                                })
                        )
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createPearlCannonCommand() {
        String ruleValue = EUNCarpetSettings.pearlCannonEnabled;
        return Commands.literal("pearlCannon")
                .requires(source -> checkPermission(source, ruleValue))
                .then(Commands.literal("set")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    PearlCannonManager.getInstance().getSchemeNames().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    PearlCannonScheme scheme = PearlCannonManager.getInstance().getScheme(name);
                                    if (scheme == null) {
                                        ctx.getSource().sendFailure(Component.literal("不存在的方案名: " + name));
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("用法: " + getSchemeUsage(scheme)));
                                    }
                                    return 0;
                                })
                                .then(Commands.argument("args", StringArgumentType.greedyString())
                                        .executes(EUNCommands::executeSet))));
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        String argsStr = StringArgumentType.getString(ctx, "args");
        PearlCannonScheme scheme = PearlCannonManager.getInstance().getScheme(name);
        if (scheme == null) throw NO_SUCH_SCHEME.create();

        String[] args = argsStr.split("\\s+");
        List<PearlCannonScheme.Field> fields = scheme.getFields();
        if (args.length != fields.size()) {
            ctx.getSource().sendFailure(Component.literal("参数数量错误，用法: " + getSchemeUsage(scheme)));
            return 0;
        }

        StringBuilder fullBinaryBuilder = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            PearlCannonScheme.Field field = fields.get(i);
            String input = args[i];
            Component error = field.validateInput(input);
            if (error != null) {
                ctx.getSource().sendFailure(Component.literal("参数 " + (i + 1) + " (" + field.getDisplayName() + ") 错误: ").append(error));
                return 0;
            }
            try {
                String encoded = field.encode(input);
                fullBinaryBuilder.append(encoded);
            } catch (IllegalArgumentException e) {
                ctx.getSource().sendFailure(Component.literal("参数 " + (i + 1) + " 编码失败: " + e.getMessage()));
                return 0;
            }
        }

        String fullBinary;
        if (scheme.getOutputOrder() != null && !scheme.getOutputOrder().isEmpty()) {
            StringBuilder orderedBinary = new StringBuilder();
            for (String fieldName : scheme.getOutputOrder()) {
                int idx = -1;
                for (int i = 0; i < fields.size(); i++) {
                    if (fields.get(i).getName().equals(fieldName)) { idx = i; break; }
                }
                if (idx == -1) {
                    ctx.getSource().sendFailure(Component.literal("内部错误: outputOrder 字段 " + fieldName + " 未找到"));
                    return 0;
                }
                String encoded = scheme.getField(fieldName).encode(args[idx]);
                orderedBinary.append(encoded);
            }
            fullBinary = orderedBinary.toString();
        } else {
            fullBinary = fullBinaryBuilder.toString();
        }

        if (fullBinary.length() != scheme.getTotalBits()) {
            ctx.getSource().sendFailure(Component.literal("内部错误: 生成的二进制长度与总位数不匹配"));
            return 0;
        }

        StringBuilder formattedBuilder = new StringBuilder();
        int pos = 0;
        for (PearlCannonScheme.Field field : fields) {
            int bits = field.getBits();
            String part = fullBinary.substring(pos, pos + bits);
            formattedBuilder.append(part).append(' ');
            pos += bits;
        }
        String finalFormatted = formattedBuilder.toString().trim();

        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("§6========== EUN 珍珠炮转译码 =========="), false);
        source.sendSuccess(() -> Component.literal("§b方案: §a" + name), false);
        source.sendSuccess(() -> Component.literal("§b完整二进制: §e" + fullBinary), false);
        source.sendSuccess(() -> Component.literal("§b格式化: §a" + finalFormatted), false);
        source.sendSuccess(() -> Component.literal("§6===================================="), false);
        return 1;
    }

    private static String getSchemeUsage(PearlCannonScheme scheme) {
        StringBuilder sb = new StringBuilder("/eun pearlCannon set ").append(scheme.getName());
        for (PearlCannonScheme.Field field : scheme.getFields()) {
            sb.append(" <").append(field.getDisplayName());
            if (field instanceof PearlCannonScheme.SumField sumField) {
                sb.append(" 0-").append(sumField.getMaxSum());
            } else if (field instanceof PearlCannonScheme.EnumField enumField) {
                String options = String.join(",", enumField.getValidInputs());
                sb.append(" [").append(options).append("]");
            }
            sb.append(">");
        }
        return sb.toString();
    }
}
