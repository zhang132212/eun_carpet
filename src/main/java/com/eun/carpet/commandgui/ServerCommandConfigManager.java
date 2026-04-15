package com.eun.carpet.commandgui;

import com.eun.carpet.EUNCarpetMod;
import com.eun.carpet.config.EUNConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理服务端 command-gui 指令预设配置。
 * <p>
 * 服务器启动时从 {@code config/eun_carpet/command_gui_presets.json} 加载配置，
 * 玩家加入时将预设推送给安装了 easy-cmd（main-sync 版本）的客户端。
 */
public class ServerCommandConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(EUNCarpetMod.MOD_ID + "|CmdGUI");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_FILENAME = "command_gui_presets.json";

	private static List<GroupDto> groups = new ArrayList<>();

	/** 加载配置，若不存在则生成默认配置文件。 */
	public static void load(Path configDir) {
		Path configPath = configDir.resolve(CONFIG_FILENAME);
		if (!Files.exists(configPath)) {
			createDefaultConfig(configPath);
		}
		reload(configDir);
	}

	/** 重新从文件加载配置（不重建默认文件）。 */
	public static void reload(Path configDir) {
		Path configPath = configDir.resolve(CONFIG_FILENAME);
		try (BufferedReader reader = Files.newBufferedReader(configPath)) {
			PresetFile file = GSON.fromJson(reader, PresetFile.class);
			groups = (file != null && file.groups != null) ? file.groups : new ArrayList<>();
			LOGGER.info("已加载 {} 个指令组（command-gui 同步）", groups.size());
		} catch (IOException | JsonParseException e) {
			LOGGER.error("加载 command_gui_presets.json 失败", e);
			groups = new ArrayList<>();
		}
	}

	/** 将预设指令推送给指定玩家，在玩家加入时调用。 */
	public static void sendToPlayer(ServerPlayer player) {
		if (groups.isEmpty()) return;
		ServerPlayNetworking.send(player, new ServerCommandPayload(new ArrayList<>(groups)));
	}

	/** 重载配置文件并将最新预设推送给所有在线玩家。 */
	public static void reloadAndPush(net.minecraft.server.MinecraftServer server) {
		reload(EUNConfigManager.getConfigRoot());
		server.getPlayerList().getPlayers().forEach(ServerCommandConfigManager::sendToPlayer);
	}

	private static void createDefaultConfig(Path configPath) {
		try {
			Files.createDirectories(configPath.getParent());

			EntryDto tp = new EntryDto("切换旁观", List.of("/!!s"), "切换到旁观模式（再次执行切换回原模式）");
			EntryDto time = new EntryDto("我在这", List.of("/!!!here"), "高亮自己");
			GroupDto exampleGroup = new GroupDto("实用工具", List.of(tp, time));

			PresetFile defaultFile = new PresetFile(List.of(exampleGroup));
			try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
				GSON.toJson(defaultFile, writer);
			}
			LOGGER.info("已生成默认配置文件：{}", configPath);
		} catch (IOException e) {
			LOGGER.error("生成默认配置文件失败", e);
		}
	}

	// ── Gson 反序列化用内部数据类（包级可见，供 ServerCommandPayload 引用）──

	static class PresetFile {
		List<GroupDto> groups;
		PresetFile() {}
		PresetFile(List<GroupDto> groups) { this.groups = new ArrayList<>(groups); }
	}

	static class GroupDto {
		String name;
		List<EntryDto> commands;
		GroupDto() {}
		GroupDto(String name, List<EntryDto> commands) {
			this.name = name;
			this.commands = new ArrayList<>(commands);
		}
	}

	static class EntryDto {
		String name;
		String description;
		List<String> commands;
		EntryDto() {}
		EntryDto(String name, List<String> commands, String description) {
			this.name = name;
			this.commands = new ArrayList<>(commands);
			this.description = description;
		}
	}
}
