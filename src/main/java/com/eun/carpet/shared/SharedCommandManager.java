package com.eun.carpet.shared;

import com.eun.carpet.EUNCarpetSettings;
import com.eun.carpet.config.EUNConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SharedCommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|SharedCmd");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_FILE = EUNConfigManager.getConfigRoot().resolve("shared_commands.json");
    private static final Type DATA_TYPE = new TypeToken<Map<String, SharedCommand>>(){}.getType();

    // 硬编码黑名单（禁止的命令关键词）
    private static final String[] COMMAND_BLACKLIST = {
            "op", "ban", "stop", "deop", "pardon", "save-all", "save-off", "save-on"
    };

    private static final Map<String, SharedCommand> COMMANDS = new ConcurrentHashMap<>();
    private static MinecraftServer server;

    public static void init(MinecraftServer serverInstance) {
        server = serverInstance;
        load();
    }

    public static void load() {
        if (!Files.exists(DATA_FILE)) {
            COMMANDS.clear();
            save();
            return;
        }
        try (Reader reader = new FileReader(DATA_FILE.toFile())) {
            Map<String, SharedCommand> loaded = GSON.fromJson(reader, DATA_TYPE);
            COMMANDS.clear();
            if (loaded != null) {
                COMMANDS.putAll(loaded);
            }
            LOGGER.info("Loaded {} shared commands", COMMANDS.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load shared commands", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            try (Writer writer = new FileWriter(DATA_FILE.toFile())) {
                GSON.toJson(COMMANDS, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save shared commands", e);
        }
    }

    /** 获取所有共享命令（返回副本） */
    public static List<SharedCommand> getAllCommands() {
        return new ArrayList<>(COMMANDS.values());
    }

    /** 获取某个玩家上传的命令 */
    public static List<SharedCommand> getCommandsByOwner(UUID ownerUuid) {
        List<SharedCommand> result = new ArrayList<>();
        for (SharedCommand cmd : COMMANDS.values()) {
            if (cmd.getOwnerUuid().equals(ownerUuid)) {
                result.add(cmd);
            }
        }
        return result;
    }

    public static SharedCommand getCommand(String id) {
        return COMMANDS.get(id);
    }

    /** 验证命令是否合法（黑名单、长度、格式） */
    public static String validateCommand(String name, List<String> commands, String description) {
        if (name == null || name.trim().isEmpty()) return "命令名称不能为空";
        if (commands == null || commands.isEmpty()) return "命令列表不能为空";
        if (description != null && description.length() > 200) return "描述过长（最多200字符）";
        if (name.length() > 50) return "命令名称过长（最多50字符）";
        // 检查名称是否已存在（全服唯一）
        for (SharedCommand existing : COMMANDS.values()) {
            if (existing.getName().equalsIgnoreCase(name.trim())) {
                return "命令名称已存在";
            }
        }
        // 检查每个命令
        for (String cmdLine : commands) {
            if (cmdLine.length() > EUNCarpetSettings.sharedCommandMaxLength) {
                return "命令过长（最大 " + EUNCarpetSettings.sharedCommandMaxLength + " 字符）";
            }
            if (!cmdLine.startsWith("/")) {
                return "每条命令必须以 / 开头";
            }
            // 黑名单检查（使用硬编码常量）
            for (String black : COMMAND_BLACKLIST) {
                if (cmdLine.toLowerCase().contains("/" + black.toLowerCase())) {
                    return "命令包含禁止词汇: " + black;
                }
            }
        }
        return null; // 验证通过
    }

    /** 上传新命令（直接通过验证后添加） */
    public static String addCommand(ServerPlayer uploader, String name, List<String> commands, String description) {
        if (!EUNCarpetSettings.sharedCommandsEnabled) {
            return "共享命令功能已禁用";
        }
        String error = validateCommand(name, commands, description);
        if (error != null) return error;
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        SharedCommand cmd = new SharedCommand(id, name.trim(), commands,
                description == null ? "" : description.trim(),
                uploader.getUUID(), uploader.getName().getString(), now);
        COMMANDS.put(id, cmd);
        save();
        broadcastUpdate("ADDED", cmd);
        return null; // 成功
    }

    /** 编辑命令（仅上传者或 OP 可编辑） */
    public static String editCommand(ServerPlayer editor, String id, String newName, List<String> newCommands, String newDescription) {
        if (!EUNCarpetSettings.sharedCommandsEnabled) return "共享命令功能已禁用";
        SharedCommand cmd = COMMANDS.get(id);
        if (cmd == null) return "命令不存在";
        if (!isOperator(editor) && !cmd.getOwnerUuid().equals(editor.getUUID())) {
            return "你没有权限编辑此命令";
        }
        // 如果名称改变，检查唯一性
        if (!cmd.getName().equals(newName.trim())) {
            for (SharedCommand existing : COMMANDS.values()) {
                if (existing.getName().equalsIgnoreCase(newName.trim()) && !existing.getId().equals(id)) {
                    return "命令名称已存在";
                }
            }
        }
        String error = validateCommand(newName, newCommands, newDescription);
        if (error != null) return error;
        cmd.setName(newName.trim());
        cmd.setCommands(newCommands);
        cmd.setDescription(newDescription == null ? "" : newDescription.trim());
        cmd.setLastEditTime(System.currentTimeMillis());
        cmd.setEditorUuid(editor.getUUID());
        cmd.setEditorName(editor.getName().getString());
        save();
        broadcastUpdate("MODIFIED", cmd);
        return null;
    }

    /** 删除命令（仅上传者或 OP） */
    public static String deleteCommand(ServerPlayer deleter, String id) {
        if (!EUNCarpetSettings.sharedCommandsEnabled) return "共享命令功能已禁用";
        SharedCommand cmd = COMMANDS.get(id);
        if (cmd == null) return "命令不存在";
        if (!isOperator(deleter) && !cmd.getOwnerUuid().equals(deleter.getUUID())) {
            return "你没有权限删除此命令";
        }
        COMMANDS.remove(id);
        save();
        broadcastUpdate("REMOVED", cmd);
        return null;
    }

    /** 广播更新给所有在线玩家 */
    private static void broadcastUpdate(String action, SharedCommand cmd) {
        if (server == null) return;
        SharedCommandUpdatePayload payload = new SharedCommandUpdatePayload(action, cmd);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * 检查玩家是否有管理权限（基于 Carpet 规则 sharedCommandManagePermission）
     * 规则值：true 表示所有玩家都有权限；false 表示没有；数字表示需要的 OP 等级
     */
    private static boolean isOperator(ServerPlayer player) {
        String permRule = EUNCarpetSettings.sharedCommandManagePermission;
        if (permRule.equals("true")) return true;
        if (permRule.equals("false")) return false;
        if (permRule.equals("ops")) {
            var ops = player.level().getServer().getPlayerList().getOps();
            return ops.get(player.nameAndId()) != null;
        }
        try {
            int requiredLevel = Integer.parseInt(permRule);
            int playerLevel = 0;
            var ops = player.level().getServer().getPlayerList().getOps();
            var entry = ops.get(player.nameAndId());
            if (entry != null) {
                playerLevel = entry.permissions().level().id();
            }
            return playerLevel >= requiredLevel;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 发送全量命令列表给指定玩家 */
    public static void sendFullListToPlayer(ServerPlayer player) {
        List<SharedCommand> all = getAllCommands();
        SharedCommandListPayload payload = new SharedCommandListPayload(all);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }
}