package com.eun.carpet.packet;

import com.eun.carpet.EUNCarpetSettings;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;

import java.util.UUID;

public class PacketCommand {
    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        return SharedSuggestionProvider.suggest(server.getPlayerNames(), builder);
    };

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("packet")
                .requires(PacketCommand::checkPermission)
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .suggests(PLAYER_SUGGESTIONS)
                                .then(Commands.argument("type", IntegerArgumentType.integer(1, 5))
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(PacketCommand::executeSet)))))
                .then(Commands.literal("list")
                        .executes(PacketCommand::executeList))
                .then(Commands.literal("info")
                        .then(Commands.argument("player", EntityArgument.player())
                                .suggests(PLAYER_SUGGESTIONS)
                                .executes(PacketCommand::executeInfo)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .suggests(PLAYER_SUGGESTIONS)
                                .executes(PacketCommand::executeRemove)));
    }

    private static boolean checkPermission(CommandSourceStack source) {
        String rule = EUNCarpetSettings.packetEnabled;
        if (rule.equals("true")) {
            return true;
        } else if (rule.equals("false")) {
            return false;
        } else {
            try {
                int requiredLevel = Integer.parseInt(rule);
                if (source.getEntity() == null) {
                    return true;
                }
                ServerPlayer player = source.getPlayer();
                if (player != null) {
                    var profile = player.getGameProfile();
                    NameAndId nameAndId = new NameAndId(profile.id(), profile.name());
                    var ops = source.getServer().getPlayerList().getOps();
                    ServerOpListEntry entry = ops.get(nameAndId);
                    if (entry != null) {
                        int playerLevel = entry.permissions().level().id();
                        return playerLevel >= requiredLevel;
                    }
                }
                return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    private static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        UUID uuid = player.getUUID();
        String name = player.getName().getString();

        int type = IntegerArgumentType.getInteger(context, "type");
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        PacketConfig config = new PacketConfig(name, type, enabled);
        PacketManager.setConfig(uuid, config);

        context.getSource().sendSuccess(() -> Component.literal("§a已设置假人 " + name + " 的打包配置: 种类数=" + type + ", enabled=" + enabled), false);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        var configs = PacketManager.getAllConfigs();
        if (configs.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§e暂无任何假人打包配置"), false);
            return 1;
        }
        context.getSource().sendSuccess(() -> Component.literal("§6=== 假人打包配置列表 ==="), false);
        configs.forEach((uuid, config) -> {
            String status = config.isEnabled() ? "§a启用" : "§c禁用";
            context.getSource().sendSuccess(() -> Component.literal("§e" + config.getName() + " §7(" + uuid + ") " + status + " §7种类数=" + config.getType()), false);
        });
        return 1;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        UUID uuid = player.getUUID();
        String name = player.getName().getString();

        PacketConfig config = PacketManager.getConfig(uuid);
        if (config == null) {
            context.getSource().sendSuccess(() -> Component.literal("§e该玩家没有打包配置"), false);
            return 1;
        }
        String status = config.isEnabled() ? "§a启用" : "§c禁用";
        context.getSource().sendSuccess(() -> Component.literal("§6=== " + name + " 的打包配置 ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7UUID: " + uuid), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7状态: " + status), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7打包种类数(type): " + config.getType()), false);
        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        UUID uuid = player.getUUID();
        String name = player.getName().getString();

        PacketManager.removeConfig(uuid);
        context.getSource().sendSuccess(() -> Component.literal("§a已删除假人 " + name + " 的打包配置"), false);
        return 1;
    }
}